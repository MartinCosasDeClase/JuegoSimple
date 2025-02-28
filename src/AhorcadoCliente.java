import java.io.*;
import java.net.*;

public class AhorcadoCliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PUERTO);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                System.out.println(mensaje);

                if (mensaje.startsWith("TURNO")) {
                    System.out.print("Ingresa una letra: ");
                    String letra = teclado.readLine();
                    out.println(letra);
                }

                if (mensaje.contains("¡Juego terminado!")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Se perdió la conexión con el servidor.");
        }
    }
}
