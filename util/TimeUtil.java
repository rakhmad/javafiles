package util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TimeUtil {
    public static Timestamp getTimestamp(String time) {
        Timestamp ts = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            ts = new Timestamp(sdf.parse(time).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return ts;
    }
}

