package cc.christopherchase;

import java.io.*;
import java.util.List;
import java.util.Scanner;


public class Main {

    public static void main(String... args){
        Main m = new Main();
        m.init();

    }

    public void init() {
        Database<User> db = new Database<>(User.class);

        /*
        All three predicates below are tested against before each row is added.

        The first two can be thought of as a simple filter. Any row that matches
        one of the predicates will not be added to the DB.

        The third allows a single row matching the predicate to be added. After that,
        no other rows which match are allowed. This can be used to ensure uniqueness.
         */
        db.addRowRequirementPredicate(user -> !user.getUsername().equals("admin"), "Protect admin username");
        db.addRowRequirementPredicate(user -> !user.getEmail().contains(".tv"), "Do not allow any .tv emails");
        db.addUniqueRowRequirement(user -> user.getUsername().contains("chris"));


        File input = new File("MOCK_DATA.csv");
        FileReader fr = null;
        BufferedReader br = null;
        try {
             fr = new FileReader(input);
             br = new BufferedReader(fr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        br.lines().forEach(s -> {
            String[] data = s.split(",");
            User u = new User();
            u.setFirstname(data[0]);
            u.setLastname(data[1]);
            u.setEmail(data[2]);
            u.setUsername(data[3]);
            u.setNum(db.rowCount() + 1);
            db.addRow(u);
        });

        System.out.println(db.rowCount());
        Scanner scan = new Scanner(System.in);


        // Building a query. If the entered query doesn't match an indexed row, use the lambda defined.
        DBQuery<User> email =  db.buildQuery(user -> val -> user.getEmail().contains(val.toString()));
        DBQuery<User> id = db.buildQuery(user -> val -> user.getNum() != 35);


        while(true){

            System.out.print("Enter query: ");

            String query = scan.nextLine();

            /*
            With query objects pre-defined, the user's input could be parsed
            and the appropriate query object used.

            i.e. <Query Type><comma><Query Value>
             */
            //List<User> idsFound = id.find(Integer.valueOf(query));

            long start = System.currentTimeMillis();

            List<User> emailsFound = email.find(query);
            long stop = System.currentTimeMillis();
            System.out.println("Search took: " + (stop - start) + "ms found " +emailsFound.size() + " " + emailsFound.toString() );

        }


    }







}
