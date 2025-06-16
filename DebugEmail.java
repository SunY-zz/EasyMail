import cn.sunyblog.easymail.exception.EmailExceptionHandler;

public class DebugEmail {
    public static void main(String[] args) {
        System.out.println("Testing email validation:");
        System.out.println("test@example.com: " + EmailExceptionHandler.isValidEmail("test@example.com"));
        System.out.println("user.name+tag@domain.co.uk: " + EmailExceptionHandler.isValidEmail("user.name+tag@domain.co.uk"));
    }
}