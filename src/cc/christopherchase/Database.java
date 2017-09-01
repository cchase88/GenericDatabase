package cc.christopherchase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class Database<T>{

    private Class<T> rowDef;

    /*
    Maps columns annotated with @Index to
    their respective row objects.
    */
    private Map<Object,T> indexMap = new HashMap<>();

    private Map<Predicate<T>, String> predicateName = new HashMap<>();
    private List<T> rows = new LinkedList<>();


    public Database(Class<T> rowDef){
        this.rowDef = rowDef;

        // Default predicate. Row is not null object.
        addRowRequirementPredicate(t -> t != null, "Null object check");

    }

    /*
    A predicate that the row must pass in order for it to be added.
     */
    public void addRowRequirementPredicate(Predicate<T> p){
        predicateName.put(p,"");
    }
    public void addRowRequirementPredicate(Predicate<T> p, String desc){
        predicateName.put(p, desc);
    }

    /*
    This method defines a unique row requirement predicate by combining the
    results of applying the predicate against all existing rows as well
    as the to-be-added row.

    Example usage:
        db.addUniqueRowRequirement(user -> user.getEmail().contains(".au"), "Single AU email");

    In the example, the database can only have a single user with an .au email address.

     */
    public void addUniqueRowRequirement(Predicate<T> p){
        addUniqueRowRequirement(p,"");
    }
    public void addUniqueRowRequirement(Predicate<T> p, String desc){

        addRowRequirementPredicate(row -> {
            boolean noneMatch = false;
            boolean isCandidate = p.test(row);
            if(isCandidate) {
                noneMatch = rows.stream().noneMatch(p);
            }

            /*
            If this row is a candidate, defer to noneMatch's result.
            If not a candidate, it's not applicable for uniqueness.
             */
            return !isCandidate || noneMatch;

        }, desc);
    }


    /*
    Given a row object, this method first tests the row
    against each of the predicates defined.

    If any predicate test fails, the row is not added.

    If all predicate tests pass, the row is first added
    to the rows list and then any index keys added to
    the index map with the row as the value.
     */
    public void addRow(T row){
        boolean allPassed = true;
        for(Predicate<T> predicate : predicateName.keySet()){
            if(!predicate.test(row)){
                allPassed = false;
                break; // No need to test the rest.
            }
        }

        if(allPassed){
            rows.add(row);

            for(Field f : indexFields()){
                try {
                    f.setAccessible(true);
                    Object rowFieldVal = f.get(row);
                    indexMap.put(rowFieldVal, row);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /*
    Using the class that defines a row (T), this method
    builds a list of fields that have been annotated with `Index`
     */

    private List<Field> indexFields;
    private List<Field> indexFields(){
        if(indexFields != null) return indexFields;

        List<Field> fieldList = new ArrayList<>();
        Field[] fields = rowDef.getDeclaredFields();
        for(Field f : fields){
            Annotation index = f.getAnnotation(Index.class);
            if(index != null) {
                f.setAccessible(true);
                fieldList.add(f);
            }
        }

        indexFields = fieldList;
        return fieldList;
    }

    /*
    This method takes a value and a predicate.

    If the value is an index, we return the object associated
    with that index.

    If the value is not an index, we search through the rows,
    filtering them using the predicate.
     */
    public List<T> findRows(Object val, Predicate<T> pred){
        List<T> results = new ArrayList<>();

        if(indexMap.containsKey(val))
            results.add(indexMap.get(val));
        else
            results = rows.stream().filter(pred).collect(Collectors.toList());


        return results;
    }


    /*
    This method allows for the reuse of a defined query by encapsulating
    it within its own object and then binding the `val` argument at a
    later time.

    Example usage:
        emailQuery = db.buildQuery(user -> val -> user.getEmail().contains(val.toString()));

    Internally, the query object makes use of the findRows method defined within this class.
     */
    public DBQuery<T> buildQuery(Function<T,Function<Object,Boolean>> func){
        return new DBQuery(this, func);
    }


    /*
    The number of rows stored in the database.
     */
    public Integer rowCount(){
        return rows.size();
    }
}
