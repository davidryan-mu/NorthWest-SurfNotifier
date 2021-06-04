package DavidR.MSW_API_Consumer;

// Import types
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

// Imports for building request and handling response
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

// Imports for cleaning Json data
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	JsonParser jp = new JsonParser();
    	
    	//Key = beach, Value = data
    	HashMap<String, JsonArray> beachResponseMap = new HashMap<String, JsonArray>();
    	Multimap<String, Forecast> surfableBeaches = ArrayListMultimap.create();
    	Forecast forecast = new Forecast();
    	
    	// Get Json from API call and populate HashMap
    	// beachResponseMap = mswRequestAndResponse(beachResponseMap, jp);
    	
    	// Get Json from local file and populate HashMap
    	beachResponseMap.put("Pipeline", readJsonFromFile("pipeline_616_forecast.json", jp));
    	beachResponseMap.put("Fistral North", readJsonFromFile("fistral_north_1_forecast.json", jp));
    	//beachResponseMap.put("Snapper Rocks", readJsonFromFile("snapper_rocks_1014_forecast.json", jp));
    	//beachResponseMap.put("Trestles", readJsonFromFile("trestles_291_forecast.json", jp));

    	
        // Prettifying, printing and filtering
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je;
        String prettyJsonString;
        
        System.out.println("ALL BEACH DATA FOR SELECTED BEACHES IN NORTHWEST IRELAND");
        
        // Loop through each beach
        for (String beach : beachResponseMap.keySet()) {
        	//Get value for current key
        	JsonArray beachData = beachResponseMap.get(beach);
        	
        	// Generate string for print
        	je = jp.parse(beachData.toString());
        	prettyJsonString = gson.toJson(je);
        	
        	System.out.println("¬ " + beach.toUpperCase() + "----------");
        	System.out.println(prettyJsonString);
        	System.out.println();
        	
        	// Determine whether beach is surfable or not based on data at each capture (timestamp)
        	for (int i = 0; i < beachData.size(); i++) {
        		forecast = gson.fromJson(beachData.get(i), Forecast.class);
        		if(isSurfable(forecast))
        				surfableBeaches.put(beach, forecast);
        	}
        }
        
        HashMap<String, String> pushNotifContent = createPushNotifContent(surfableBeaches); 
        
        for (String header : pushNotifContent.keySet()) {
        	System.out.println(header.toUpperCase());
        	System.out.println(pushNotifContent.get(header));
        	System.out.println();
        }
        
        pushNotification(pushNotifContent, gson);
    }
    
    // Takes an empty hash map and populates it through a call to the MSW API
    public static HashMap<String, JsonArray> mswRequestAndResponse(HashMap<String, JsonArray> beachResponseMap, JsonParser jp) throws Exception{
    	// Set up variables for making request and receiving response
    	Keys keys = new Keys();
        String host = "https://magicseaweed.com/api/";
        String msw_api_key = keys.MSW_KEY;
        
        HashMap<String, String> beachMap = new HashMap<String, String>();
        beachMap.put("Rossnowlagh", "1082");
        beachMap.put("Tullan", "4539");
        beachMap.put("Bundoran Peak", "50");
        beachMap.put("Dooey", "4538");
        beachMap.put("Leenan Beach", "8114");
        
        HttpResponse<JsonNode> mswResponse;       
        
        // Send request and handle response
        for(String beach : beachMap.keySet()) {
        	mswResponse = Unirest.get(host + msw_api_key + "/forecast/?spot_id=" + beachMap.get(beach)).asJson();
        	JsonElement parsedElement = jp.parse(mswResponse.getBody().getArray().toString());
        	JsonArray responseObj = (JsonArray) parsedElement;
        	beachResponseMap.put(beach, responseObj);
        }
        
        return beachResponseMap;
    }
    
    // Takes a file name and Json parser and parses the file content to a Json array
    public static JsonArray readJsonFromFile(String fileName, JsonParser jp) throws Exception {
    	JsonElement jsonArray = jp.parse(new FileReader(fileName));
    	
    	return (JsonArray) jsonArray;
    }
    
    public static Boolean isSurfable(Forecast forecast) throws Exception {
    	Boolean surfable = false; 
    			
    	if (forecast.swell.absMaxBreakingHeight <= 7.0
    			&& forecast.swell.absMinBreakingHeight >= 2.0
    			&& forecast.wind.compassDirection.contains("W")
    			&& forecast.wind.speed >= 2
    			&& forecast.wind.speed <= 8
    			&& forecast.solidRating >= 1)
    		surfable = true;

    	return surfable;
    }
    
    public static void pushNotification(HashMap<String, String> contentObj, Gson gson) throws Exception {
    	Keys keys = new Keys();
    	String content = contentObj.get("content") != null ? contentObj.get("content") : "Nothing to show for today...";
    	String pushContent = contentObj.get("pushContent");
    	
    	// Set up push notification via spontit
        String user_id = keys.SPONTIT_USER_ID;
        String spontit_api_key = keys.SPONTIT_KEY;
        String payload = "{  \"content\": \"" + content + "\", \"pushContent\": \"" + pushContent + "\"}";
        
        // Send push notification
        HttpResponse <JsonNode> spontitResponse = Unirest.post("https://api.spontit.com/v3/push")
        		.header("X-Authorization", spontit_api_key)
        		.header("X-UserId", user_id)
        		.body(payload)
        		.asJson();
        
        // Push notification response
        System.out.println("Spontit response: \n" + gson.toJson(spontitResponse.getBody().toString()));
    }
    
    public static String unixToDateFormat(int unixTime) {
    	Date date = new Date(unixTime*1000);
    	SimpleDateFormat sdf = new SimpleDateFormat("E HH:mm");
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    	String format = sdf.format(date);
    	return format;
    }
    
    public static HashMap<String, String> createPushNotifContent(Multimap<String, Forecast> surfableBeaches) {
    	HashMap<String, String> finalPush = new HashMap<String, String>();
    	
    	String beachesForPushContent = "";      
        HashMap<String, String> beachesForContent = new HashMap<String, String>();
        
        // Set up description content, beaches may have multiple occurrences in keyset of Multimap so use HashMap to ensure each beach is only one key
        surfableBeaches.forEach((beach, thisForecast) -> {    	
        	if (!beachesForContent.containsKey(beach)) {
        		// Add timestamp if new key
        		beachesForContent.put(beach, unixToDateFormat(thisForecast.localTimestamp));
        	} else {
        		// Append timestamp if key already exists
        		beachesForContent.put(beach, beachesForContent.get(beach) + ", " + unixToDateFormat(thisForecast.localTimestamp));
        	}
        });
        
        // Create one string from all string values
        for (String beach: beachesForContent.keySet()) {
        	finalPush.put("content", (finalPush.get("content") != null ? finalPush.get("content") : "")
        			+ beach + " is surfable at " + beachesForContent.get(beach) + "\\n\\n");
        }
        
        // Set up push content for notification header
        if (beachesForContent.keySet().size() == 0) {
        	// No surfable beaches
        	beachesForPushContent = "Not seeing any good conditions for a surf today :(\\nTry a spin or a hike instead...";
        } else if (beachesForContent.keySet().size() == 1) {
        	// One surfable beach, trim square brackets from converting keyset of size one to string
        	String singularBeach = beachesForContent.keySet().toString();
        	beachesForPushContent = singularBeach.substring(1, singularBeach.length()-1) + " should be good for a surf today!";
        } else {
        	// More than one surfable beach, counter for identifying last beach and prefixing with 'and'
        	int beachCount = 0;
        	for (String beach : beachesForContent.keySet()) {
        		if (beachCount == beachesForContent.keySet().size()-1) {
                	beachesForPushContent = beachesForPushContent + "and " + beach;
        		} else {
                	beachesForPushContent = beachesForPushContent + beach + ", ";
        		}
        		
        		beachCount++;
        	}
        	
        	beachesForPushContent = beachesForPushContent + " are worth checking out today!";
        }
        
        finalPush.put("pushContent", beachesForPushContent);
        
        return finalPush;
    }
}
