import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;


public class Sort {
    public static void main(String args[]) {
    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	String line = null;
    	String word = null;
    	String wordCounter = null;
    	TreeMap<String, ArrayList<String>> map = new TreeMap<String, ArrayList<String>>();
    	
    	try {
			while((line = br.readLine()) != null) {
				if(line == "") {
					continue;
				}
				String[] parts = line.split("\t");
				word = parts[0];
				wordCounter = parts[1]; 
				
				ArrayList<String> list = map.get(word); 
				if(list == null) {
					ArrayList<String> newList = new ArrayList<String>();
					newList.add(wordCounter);
					map.put(word, newList);
				} else {
					list.add(wordCounter);
					map.put(word, list);
				}
			}
			
			for(Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
				ArrayList<String> list = entry.getValue();
				for(int i = 0; i < list.size(); i++) {
			        System.out.println(entry.getKey() + "\t" + list.get(i));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
