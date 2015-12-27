import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Base64;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Bing{

	private static String accountId = "dZHx2LeVZb9BHorIDiwiIPTetFDaeqOgBHBJAScDzFE";

	public static void main(String[] args) throws IOException, JSONException {
		String searchPhrase = "'what is spelunking'";
		int size = 5;

        //Paging requires the use of $top and $size query params
		final String bingUrlPattern = "https://api.datamarket.azure.com/Bing/Search/Web?$top="+size+"&$skip=0&$format=JSON&Query=%%27%s%%27";

		final String query = URLEncoder.encode(searchPhrase, Charset.defaultCharset().name());
		final String bingUrl = String.format(bingUrlPattern, query);

		final String accountKeyEnc = Base64.getEncoder().encodeToString((accountId + ":" + accountId).getBytes());

		final URL url = new URL(bingUrl);
		final URLConnection connection = url.openConnection();
		connection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);

		try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String inputLine;
			final StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			final JSONObject json = new JSONObject(response.toString());
			final JSONObject d = json.getJSONObject("d");
			final JSONArray results = d.getJSONArray("results");
			final int resultsLength = results.length();
			for (int i = 0; i < resultsLength; i++) {
				final JSONObject result = results.getJSONObject(i);
                String resultUrl = result.getString("Url");
                String title = result.getString("Title");
				System.out.println("\n" + (i+1) + "\n"+ title + "\n" + resultUrl);
			}
		}

	}

}
