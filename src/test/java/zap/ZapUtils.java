package zap;

import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;

import static java.lang.Integer.valueOf;

public class ZapUtils {
    public static int getInteger(ApiResponse response) {
        return valueOf(((ApiResponseElement) response).getValue());
    }
}
