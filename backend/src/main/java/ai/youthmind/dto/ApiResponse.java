package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private int statusCode;
    private Map<String, String> headers;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        response.setStatusCode(200);
        return response;
    }
    
    public static <T> ApiResponse<T> success(T data, int statusCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        response.setStatusCode(statusCode);
        return response;
    }
    
    public static <T> ApiResponse<T> created(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        response.setStatusCode(201);
        return response;
    }
    
    public static <T> ApiResponse<T> error(T data, int statusCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        response.setStatusCode(statusCode);
        return response;
    }
}
