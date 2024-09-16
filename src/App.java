import StringList.StringList;

public class App {
    public static void main(String[] args) throws Exception {
        StringList str_list = new StringList();
        str_list.add("hello");
        str_list.add(1);
        str_list.add(1, new boolean[]{false, true, true});
        
        // StringList str_list = new StringList();
        // str_list.add("hello");

        // str_list.add()
        System.out.println(str_list);
        for (boolean temp : (boolean[]) str_list.get(1)) {
            System.out.println(temp);
        }
    }
}
