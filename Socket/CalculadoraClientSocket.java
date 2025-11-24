package Socket;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Cliente de linha de comando leve para a Calculadora remota.
 *
 * Funcionalidades:
 * - Interface textual: aceita comandos:
 *    soma a b
 *    subtracao a b
 *    multiplicacao a b
 *    divisao a b
 *    expressao1 <expr>   -> Abordagem 1: cliente converte para RPN e faz chamadas remotas para cada operação
 *    expressao2 <expr>   -> Abordagem 2: envia a expressão inteira ao servidor (op=5)
 *    exit
 *
 * - Para chamadas remotas de operações básicas, cria uma conexão por operação.
 *
 * OBS: alterar HOST se servidor estiver em outra máquina.
 */
public class CalculadoraClientSocket {
    private static final String HOST = "127.0.0.1"; // ajuste para 192.168.0.11 se necessário
    private static final int PORT = 9090;

    public static void main(String[] args) {
        System.out.println("Cliente Calculadora (digite 'help' para comandos)");
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String line = sc.nextLine();
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            // tokenizar comando
            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String rest = parts.length > 1 ? parts[1] : "";

            try {
                switch (cmd) {
                    case "soma":
                    case "subtracao":
                    case "multiplicacao":
                    case "divisao":
                        handleBasicOp(cmd, rest);
                        break;
                    case "expressao1":
                        // Abordagem 1: cliente converte para RPN e faz chamadas remotas para cada operação
                        handleExpressaoAbordagem1(rest);
                        break;
                    case "expressao2":
                        // Abordagem 2: envia a expressão inteira ao servidor (op=5)
                        handleExpressaoAbordagem2(rest);
                        break;
                    default:
                        System.out.println("Comando desconhecido. Digite 'help'.");
                }
            } catch (Exception e) {
                System.out.println("Erro no cliente: " + e.getMessage());
            }
        }

        System.out.println("Cliente encerrado.");
        sc.close();
    }

    private static void printHelp() {
        System.out.println("Comandos:");
        System.out.println("  soma a b");
        System.out.println("  subtracao a b");
        System.out.println("  multiplicacao a b");
        System.out.println("  divisao a b");
        System.out.println("  expressao1 <expressao>   // cliente avalia por RPN e faz chamadas remotas para cada operacao");
        System.out.println("  expressao2 <expressao>   // envia a expressao para o servidor (op=5) e servidor avalia");
        System.out.println("  help");
        System.out.println("  exit");
    }

    // Mapeia comando textual para código de operação
    private static int opCodeFromCmd(String cmd) {
        switch (cmd) {
            case "soma": return 1;
            case "subtracao": return 2;
            case "multiplicacao": return 3;
            case "divisao": return 4;
        }
        return -1;
    }

    private static void handleBasicOp(String cmd, String rest) throws IOException {
        String[] tokens = rest.trim().split("\\s+");
        if (tokens.length != 2) { System.out.println("Uso: " + cmd + " a b"); return; }
        String a = tokens[0];
        String b = tokens[1];
        int op = opCodeFromCmd(cmd);
        String result = remoteCall(op, a, b);
        System.out.println("Resultado: " + result);
    }

    // Faz uma chamada remota simples (abre socket, envia 3 linhas: op, oper1, oper2; lê 1 linha resultado)
    private static String remoteCall(int op, String oper1, String oper2) throws IOException {
        try (Socket clientSocket = new Socket(HOST, PORT)) {
            DataOutputStream socketSaidaServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader messageFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            socketSaidaServer.writeBytes(op + "\n");
            socketSaidaServer.writeBytes(oper1 + "\n");
            socketSaidaServer.writeBytes(oper2 + "\n");
            socketSaidaServer.flush();

            String result = messageFromServer.readLine();
            return result;
        }
    }

    // === Abordagem 1 ===
    // Converte expressão -> RPN (usando shunting-yard local) e avalia RPN fazendo chamadas remotas por cada operador.
    private static void handleExpressaoAbordagem1(String expr) throws IOException {
        if (expr.trim().isEmpty()) {
            System.out.println("Uso: expressao1 <expressao>");
            return;
        }
        List<String> tokens = tokenize(expr);
        List<String> rpn;
        try {
            rpn = shuntingYard(tokens);
        } catch (Exception e) {
            System.out.println("Erro ao converter para RPN: " + e.getMessage());
            return;
        }

        Deque<String> stack = new ArrayDeque<>();
        for (String tok : rpn) {
            if (isNumber(tok)) {
                stack.push(tok);
            } else {
                // operador: sempre realizamos a operação remotamente (exige 2 operandos)
                if (stack.size() < 2) {
                    System.out.println("RPN invalido (menos de 2 operandos para operador).");
                    return;
                }
                String b = stack.pop();
                String a = stack.pop();
                int opCode = operatorToOpCode(tok);
                if (opCode == -1) {
                    System.out.println("Operador desconhecido: " + tok);
                    return;
                }
                String result = remoteCall(opCode, a, b);
                if (result == null) { System.out.println("Erro: resposta nula do servidor"); return; }
                if (result.startsWith("ERRO")) {
                    System.out.println("Servidor retornou erro: " + result);
                    return;
                }
                // push resultado (string numerica)
                stack.push(result);
            }
        }
        if (stack.size() != 1) {
            System.out.println("Erro: avaliacao RPN produziu múltiplos valores");
        } else {
            System.out.println("Resultado (Abordagem1): " + stack.pop());
        }
    }

    // === Abordagem 2 ===
    // Envia opCode 5 e oper1 = expressão inteira
    private static void handleExpressaoAbordagem2(String expr) throws IOException {
        if (expr.trim().isEmpty()) {
            System.out.println("Uso: expressao2 <expressao>");
            return;
        }
        String result = remoteCall(5, expr, "0");
        System.out.println("Resultado (Abordagem2): " + result);
    }

    // Utilitários (tokenize, shunting-yard) — praticamente o mesmo algoritmo da Calculadora.java
    private static List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        s = s.trim();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(' || c == ')' || c == '+' || c == '*' || c == '/') {
                tokens.add(String.valueOf(c));
                i++;
            } else if (c == '-') {
                if (tokens.isEmpty() || isOperator(tokens.get(tokens.size()-1)) || tokens.get(tokens.size()-1).equals("(")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append('-');
                    i++;
                    while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                        sb.append(s.charAt(i));
                        i++;
                    }
                    tokens.add(sb.toString());
                } else {
                    tokens.add("-");
                    i++;
                }
            } else if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                    sb.append(s.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
            } else {
                throw new IllegalArgumentException("Token invalido: '" + c + "'");
            }
        }
        return tokens;
    }

    private static boolean isOperator(String tok) {
        return tok.equals("+") || tok.equals("-") || tok.equals("*") || tok.equals("/");
    }

    private static boolean isNumber(String s) {
        try { Double.parseDouble(s); return true; } catch (Exception e) { return false; }
    }

    private static int prec(String op) {
        switch (op) {
            case "+": case "-": return 1;
            case "*": case "/": return 2;
        }
        return 0;
    }

    private static boolean isLeftAssoc(String op) { return true; }

    //shutting-yard algorithm desenvolvido por Dijkstra (adaptado)
    private static List<String> shuntingYard(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(token);
            } else if (isOperator(token)) {
                while (!stack.isEmpty() && isOperator(stack.peek())) {
                    String o2 = stack.peek();
                    if ((isLeftAssoc(token) && prec(token) <= prec(o2)) ||
                        (!isLeftAssoc(token) && prec(token) < prec(o2))) {
                        output.add(stack.pop());
                    } else break;
                }
                stack.push(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses");
                stack.pop();
            } else {
                throw new IllegalArgumentException("Token inesperado: " + token);
            }
        }
        while (!stack.isEmpty()) {
            String t = stack.pop();
            if (t.equals("(") || t.equals(")")) throw new IllegalArgumentException("Mismatched parentheses");
            output.add(t);
        }
        return output;
    }

    private static int operatorToOpCode(String op) {
        switch (op) {
            case "+": return 1;
            case "-": return 2;
            case "*": return 3;
            case "/": return 4;
        }
        return -1;
    }
}
