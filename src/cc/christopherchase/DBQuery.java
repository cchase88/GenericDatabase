package cc.christopherchase;

import java.util.List;
import java.util.function.Function;


public class DBQuery<T> {



    private final Database<T> db;
    private final Function<T,Function<Object,Boolean>> func;

    DBQuery(Database db, Function<T,Function<Object,Boolean>> func){
        this.db = db;
        this.func = func;
    }

    public List<T> find(Object val){
        return db.findRows(val, t -> func.apply(t).apply(val));
    }

}
