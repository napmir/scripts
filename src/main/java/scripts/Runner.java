package scripts;

public class Runner {

    public static void main(String[] args) {
        RetrieveProducts retrieveProducts = new RetrieveProducts();
        String url = "";
        String pathForCSV = "";
        retrieveProducts.start(url, pathForCSV);
    }
}
