package Socket;
import java.io.*;
import java.net.*;

/**
 * Servidor TCP para a Calculadora.
 * - Escuta na porta 9090 (loop infinito)
 * - Lê 3 linhas por conexão: operacao, oper1, oper2
 *   operacao: 1 soma, 2 subtracao, 3 multiplicacao, 4 divisao, 5 expressao completa
 * - Caso 5: oper1 contem a expressão infixa inteira (oper2 pode ser ignorado)
 * - Responde com 1 linha contendo o resultado como String
 */
public class CalculadoraServerSocket {

    public static void main(String[] args) {
        ServerSocket welcomeSocket = null;
        Calculadora calc = new Calculadora();
        int port = 9090;

        try {
            welcomeSocket = new ServerSocket(port);
            System.out.println("Servidor no ar - porta " + port);

            while (true) {
                try (Socket connectionSocket = welcomeSocket.accept()) {
                    System.out.println("Nova conexão de " + connectionSocket.getRemoteSocketAddress());
                    BufferedReader socketEntrada = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                    DataOutputStream socketOutput = new DataOutputStream(connectionSocket.getOutputStream());

                    String operacaoStr = socketEntrada.readLine();
                    String oper1 = socketEntrada.readLine();
                    String oper2 = socketEntrada.readLine();

                    if (operacaoStr == null) {
                        socketOutput.writeBytes("ERRO: operacao nula\n");
                        socketOutput.flush();
                        continue;
                    }

                    int operacao;
                    try {
                        operacao = Integer.parseInt(operacaoStr.trim());
                    } catch (NumberFormatException nfe) {
                        socketOutput.writeBytes("ERRO: codigo de operacao invalido\n");
                        socketOutput.flush();
                        continue;
                    }

                    String result;
                    try {
                        switch (operacao) {
                            case 1: // soma
                                result = "" + calc.soma(Double.parseDouble(oper1), Double.parseDouble(oper2));
                                break;
                            case 2: // subtracao
                                result = "" + calc.subtracao(Double.parseDouble(oper1), Double.parseDouble(oper2));
                                break;
                            case 3: // multiplicacao
                                result = "" + calc.multiplicacao(Double.parseDouble(oper1), Double.parseDouble(oper2));
                                break;
                            case 4: // divisao
                                try {
                                    result = "" + calc.divisao(Double.parseDouble(oper1), Double.parseDouble(oper2));
                                } catch (ArithmeticException ae) {
                                    result = "ERRO: " + ae.getMessage();
                                }
                                break;
                            case 5: // expressão completa - oper1 contém a expressão
                                result = calc.calcularExpressao(oper1);
                                break;
                            default:
                                result = "ERRO: operacao desconhecida";
                                break;
                        }
                    } catch (Exception e) {
                        result = "ERRO: entrada invalida (" + e.getMessage() + ")";
                    }

                    socketOutput.writeBytes(result + "\n");
                    socketOutput.flush();
                    System.out.println("Resposta enviada: " + result);

                    // streams e socket serão fechados pelo try-with-resources ao término do bloco
                } catch (IOException ioeConn) {
                    System.err.println("Erro na conexão: " + ioeConn.getMessage());
                    // continuar serve loop
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (welcomeSocket != null) {
                try { welcomeSocket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
