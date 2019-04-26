import java.util.ArrayList;

public class PickPak {
    private ArrayList<Doos> volgorde;
    public static ArrayList<Item> items;
    public ArrayList<Item> pakbon;

    public PickPak() {
        items = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                items.add(new Item(new Locatie(k, new Coordinate(i,j))));
                k++;
            }
        }
        items.add(new Item(new Locatie(k, new Coordinate(4,0))));

        pakbon = new ArrayList<>();
        
        pakbon.add(items.get(2));
        pakbon.add(items.get(8));
        pakbon.add(items.get(14));
        pakbon.add(items.get(17));
        pakbon.add(items.get(23));
        pakbon.add(items.get(12));
        pakbon.add(items.get(15));
        pakbon.add(items.get(10));
        
        TSP tsp = new TSP(pakbon);
        
        System.out.println("Beste route: "+tsp.getBestRoute()+"  Afstand: "+tsp.getBestRouteDist());
        
        // BPP bpp = new BPP(items);
        // volgorde = bpp.getVolgorde();
    }

    
}
