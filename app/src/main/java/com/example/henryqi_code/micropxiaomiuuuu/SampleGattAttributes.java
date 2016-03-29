package com.example.henryqi_code.micropxiaomiuuuu;

import java.util.HashMap;

/**
 * Created by henryqi_code on 28/03/2016.
 */
public class SampleGattAttributes {
    private static HashMap<String,String> attributes = new HashMap();
    //UUID for 2 Services:
    public static String ORIENTATION_SERVICE = "42821a40-e477-11e2-82d0-0002a5d5c51b";
    public static String SAMPLE_SERVICE = "02366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    //UUID for 4 characteristics:
    public static String TEST_BUTTON = "e23e78a0-cf4a-11e1-8ffc-0002a5d5c51b"; //Free Fall
    public static String TEMP_MEASUREMENT = "";
    public static String PITCH_MEASUREMENT = "cd20c480-e48b-11e2-840b0002a5d5c51b";
    public static String ROLL_MEASUREMENT = "01c50b60-e48c-11e2-a073-0002a5d5c51b";

    static{
        // Services
        attributes.put(ORIENTATION_SERVICE,"Orientation Service");
        attributes.put(SAMPLE_SERVICE, "Sample Service");
        // Characteristics
//        attributes.put(TEMP_MEASUREMENT, "Temperature Measurement");
        attributes.put(PITCH_MEASUREMENT, "Pitch Measurement");
        attributes.put(ROLL_MEASUREMENT, "Roll Measurement");
    }

    public static String lookup(String uuid, String defaultName){
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

}
