import java.io.*;
import java.net.*;
import java.util.*;

public class AhorcadoServidor {
    private static final int PUERTO = 5000;
    private static final String[] PALABRAS = {"MARTIN", "IKER", "DANIEL", "LUISMIGUEL"};
    private static String palabraSecreta;
    private static char[] progreso;
    private static int intentosRestantes = 6;
    private static List<Socket> jugadores = Collections.synchronizedList(new ArrayList<>());
    private static Set<Character> letrasUsadas = new HashSet<>();
    private static Map<Socket, Integer> puntuaciones = new HashMap<>();
    private static boolean juegoTerminado = false;

    public static void main(String[] args) {
        palabraSecreta = PALABRAS[new Random().nextInt(PALABRAS.length)];
        progreso = new char[palabraSecreta.length()];
        Arrays.fill(progreso, '_');

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado. Esperando jugadores...");

            while (jugadores.size() < 2) {
                Socket socket = serverSocket.accept();
                synchronized (jugadores) {
                    jugadores.add(socket);
                    puntuaciones.put(socket, 0);
                }
                System.out.println("Jugador conectado. Total jugadores: " + jugadores.size());
            }

            manejarJuego();
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static void manejarJuego() {
        int turno = 0;

        while (intentosRestantes > 0 && new String(progreso).contains("_") && !juegoTerminado) {
            Socket jugadorActual;

            synchronized (jugadores) {
                if (jugadores.isEmpty()) {
                    System.out.println("Todos los jugadores se desconectaron. Terminando juego...");
                    return;
                }
                jugadorActual = jugadores.get(turno % jugadores.size());
            }

            try {
                PrintWriter out = new PrintWriter(jugadorActual.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(jugadorActual.getInputStream()));

                out.println("\nTURNO: ¡Es tu turno!");
                out.println("Palabra: " + new String(progreso));
                out.println("Letras usadas: " + letrasUsadas);
                out.println("Intentos restantes: " + intentosRestantes);
                out.println("Tu puntuación: " + puntuaciones.get(jugadorActual));
                out.println("Ingresa una letra o intenta adivinar la palabra completa:");

                String input = in.readLine();
                if (input == null) {
                    manejarDesconexion(jugadorActual);
                    continue;
                }

                input = input.trim().toUpperCase();

                if (input.length() == 1) {
                    char letra = input.charAt(0);

                    synchronized (letrasUsadas) {
                        if (letrasUsadas.contains(letra)) {
                            out.println("¡Esa letra ya fue usada! Pierdes turno.");
                        } else {
                            letrasUsadas.add(letra);
                            boolean acierto = false;

                            for (int i = 0; i < palabraSecreta.length(); i++) {
                                if (palabraSecreta.charAt(i) == letra) {
                                    progreso[i] = letra;
                                    acierto = true;
                                }
                            }

                            if (acierto) {
                                puntuaciones.put(jugadorActual, puntuaciones.get(jugadorActual) + 10);
                                out.println("¡Adivinaste una letra!");
                            } else {
                                intentosRestantes--;
                                out.println("¡Letra incorrecta! Te quedan " + intentosRestantes + " intentos.");
                            }
                        }
                    }
                } else {
                    if (input.equals(palabraSecreta)) {
                        progreso = palabraSecreta.toCharArray();
                        juegoTerminado = true;
                        anunciarGanador(jugadorActual);
                        return;
                    } else {
                        intentosRestantes -= 2;
                        out.println("¡Palabra incorrecta! Pierdes 2 intentos. Intentos restantes: " + intentosRestantes);
                    }
                }

                actualizarEstadoJugadores();
            } catch (IOException e) {
                manejarDesconexion(jugadorActual);
            }

            turno++;
        }

        finalizarJuego();
    }

    private static void manejarDesconexion(Socket jugador) {
        synchronized (jugadores) {
            System.out.println("Un jugador se desconectó.");
            jugadores.remove(jugador);
            puntuaciones.remove(jugador);
        }

        if (jugadores.isEmpty()) {
            System.out.println("No quedan jugadores. Terminando juego...");
            juegoTerminado = true;
        }
    }

    private static void actualizarEstadoJugadores() {
        synchronized (jugadores) {
            for (Socket jugador : jugadores) {
                try {
                    PrintWriter broadcast = new PrintWriter(jugador.getOutputStream(), true);
                    broadcast.println("\nEstado: " + new String(progreso));
                    broadcast.println("Letras usadas: " + letrasUsadas);
                    broadcast.println("Intentos restantes: " + intentosRestantes);
                } catch (IOException e) {
                    manejarDesconexion(jugador);
                }
            }
        }
    }

    private static void anunciarGanador(Socket ganador) {
        synchronized (jugadores) {
            for (Socket jugador : jugadores) {
                try {
                    PrintWriter broadcast = new PrintWriter(jugador.getOutputStream(), true);
                    if (jugador == ganador) {
                        broadcast.println("\n¡FELICIDADES! Adivinaste la palabra '" + palabraSecreta + "' y ganaste el juego.");
                    } else {
                        broadcast.println("\n¡Fin del juego! La palabra era '" + palabraSecreta + "'. " +
                                "El ganador es " + ganador.getRemoteSocketAddress());
                    }
                    jugador.close();
                } catch (IOException e) {
                    System.out.println("Error al cerrar conexión con un jugador.");
                }
            }
            jugadores.clear();
        }
    }

    private static void finalizarJuego() {
        synchronized (jugadores) {
            String resultado = (new String(progreso).equals(palabraSecreta))
                    ? "¡Felicidades, la palabra era '" + palabraSecreta + "'!"
                    : "Perdiste. La palabra era: " + palabraSecreta;

            for (Socket jugador : jugadores) {
                try {
                    PrintWriter broadcast = new PrintWriter(jugador.getOutputStream(), true);
                    broadcast.println("\n¡Juego terminado!");
                    broadcast.println(resultado);
                    broadcast.println("Tu puntuación: " + puntuaciones.get(jugador));
                    jugador.close();
                } catch (IOException e) {
                    System.out.println("Error al cerrar conexión con un jugador.");
                }
            }
            jugadores.clear();
        }
    }
}
