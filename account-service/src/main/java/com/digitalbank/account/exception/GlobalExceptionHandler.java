package com.digitalbank.account.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    /*Hum AccountNotFoundException ko HTTP 404 NOT FOUND response mein convert karenge.*/
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(AccountNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex)
    {
        Map<String, Object> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("messages", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    //Handle Invalid Request Parameters
    /*Handling
    accountId=abc instead of a number.
    Missing required accountId.
   Invalid query parameter types.*/
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex)
    {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Invalid Request Parameter");
        response.put("message", "Invalid value for parameter: "+ex.getName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        /*Spring automatically converts request parameters into Java types.
        If conversion fails, it throws MethodArgumentTypeMismatchException.*/
    }

    //If accountId is missing. Spring throws MissingServletRequestParameterException.
    //So we'll handle MissingServletRequestParameterException.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex)
    {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Missing required parameter");
        response.put("message", "Required parameter is missing: "+ex.getParameterName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        //Example: http://localhost:8080/api/accounts/updateAccount?accountId=
    }

    /*
    Remember the validation we added to pagination?
- Negative page number
- Page size 0
- Page size greater than 100
Currently, these throw an IllegalArgumentException.
We also need to map these consistently to an HTTP 400 response.
*/
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex)
    {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /*
    Handle Unexpected Exceptions (HTTP 500)
    Production applications mein unexpected exceptions aa sakte hain, jaise:
    - Database connectivity failure.
    - Unexpected NullPointerException.
    - Runtime errors.
    - Unhandled application exceptions.
In cases mein API ko structured HTTP 500 response return karna chahiye.
    * */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex)
    {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "Something went wrong. Please try again later");

        log.error("Unexpected application exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        /*Notice that here we are not returning ex.getMessage() to the client.
        Because Exception messages may expose SQL queries, DB table names,
        internal class names and infrastructure details.
         */
    }

    //Note: Production practice --> Keep detailed exception information in server logs, not API responses.
}
