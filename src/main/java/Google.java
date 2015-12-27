import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Google {


	public static void main(String args[]) throws IOException, JSONException {
        String searchPhrase = "'what is spelunking'";

        //Paging requires the use of start url query param
        String baseUrl = "http://ajax.googleapis.com/ajax/services/search/web?start=0&rsz=large&v=1.0&q=";


        String encodedSearchPhrase = URLEncoder.encode(searchPhrase, "UTF-8");
        URL url = new URL( baseUrl + encodedSearchPhrase);

        URLConnection connection = url.openConnection();

        try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            final StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            final JSONObject json = new JSONObject(response.toString());


            String total = json.getJSONObject("responseData")
                    .getJSONObject("cursor")
                    .getString("estimatedResultCount");


            JSONArray results = json.getJSONObject("responseData").getJSONArray("results");

            System.out.println("Displaying " + results.length() + " of Total results = " + total );

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                String resultUrl = result.getString("url");
                String title = result.getString("titleNoFormatting");
                System.out.println("" + (i+1) + "\n" + title + "\n" + resultUrl + "\n");
            }
        }
		
	}
}
