package RMI;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Implementação do objeto remoto da Calculadora via RMI.
 */
public class Calculadora implements ICalculadora {

    private static final long serialVersionUID = 1L;
    private static int chamadas = 0;   // contador de chamadas remotas

    // ================= OPERACOES BASICAS ==================

    @Override
    public int soma(int a, int b) throws RemoteException {
        System.out.println("Método soma chamado " + chamadas++);
        return a + b;
    }

    @Override
    public int subtracao(int a, int b) throws RemoteException {
        System.out.println("Método subtracao chamado " + chamadas++);
        return a - b;
    }

    @Override
    public int multiplicacao(int a, int b) throws RemoteException {
        System.out.println("Método multiplicacao chamado " + chamadas++);
        return a * b;
    }

    @Override
    public int divisao(int a, int b) throws RemoteException {
        System.out.println("Método divisao chamado " + chamadas++);
        if (b == 0)
            throw new RemoteException("Erro: Divisão por zero não permitida.");
        return a / b;
    }

    // ================== EXPRESSÕES COMPLETAS ====================

    @Override
    public int calcularExpressao(String expressao) throws RemoteException {
        System.out.println("Método calcularExpressao chamado " +
                chamadas++ + " para: " + expressao);

        try {
            List<String> tokens = tokenize(expressao);
            List<String> rpn = shuntingYard(tokens);
            int result = evalRPN(rpn);
            return result;
        } catch (Exception e) {
            throw new RemoteException("Expressão inválida: " + e.getMessage());
        }
    }

    // ====================== TOKENIZAÇÃO =======================
    private List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        s = s.replace(" ", "");

        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);

            if ("()+-*/".indexOf(c) >= 0) {
                tokens.add(String.valueOf(c));
                i++;
            }
            else if (Character.isDigit(c)) {
                StringBuilder num = new StringBuilder();
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    num.append(s.charAt(i));
                    i++;
                }
                tokens.add(num.toString());
            }
            else {
                throw new RuntimeException("Caractere inválido: " + c);
            }
        }
        return tokens;
    }

    private boolean isOperator(String t) {
        return t.equals("+") || t.equals("-") || t.equals("*") || t.equals("/");
    }

    private int precedence(String op) {
        return (op.equals("+")||op.equals("-")) ? 1 : 2;
    }

    // ============= SHUNTING-YARD -> RPN ===================
    private List<String> shuntingYard(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (String t : tokens) {
            if (t.matches("\\d+")) {
                output.add(t);
            }
            else if (isOperator(t)) {
                while (!stack.isEmpty() && isOperator(stack.peek()) &&
                        precedence(stack.peek()) >= precedence(t))
                    output.add(stack.pop());

                stack.push(t);
            }
            else if (t.equals("(")) {
                stack.push(t);
            }
            else if (t.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("("))
                    output.add(stack.pop());
                if (stack.isEmpty())
                    throw new RuntimeException("Parênteses desbalanceados");
                stack.pop();
            }
        }

        while (!stack.isEmpty())
            output.add(stack.pop());

        return output;
    }

    // ================= AVALIAÇÃO RPN =====================
    private int evalRPN(List<String> rpn) throws RemoteException {
        Stack<Integer> stack = new Stack<>();

        for (String t : rpn) {
            if (t.matches("\\d+")) {
                stack.push(Integer.parseInt(t));
            }
            else {
                int b = stack.pop();
                int a = stack.pop();

                switch (t) {
                    case "+": stack.push(soma(a,b)); break;
                    case "-": stack.push(subtracao(a,b)); break;
                    case "*": stack.push(multiplicacao(a,b)); break;
                    case "/": stack.push(divisao(a,b)); break;
                    default: throw new RuntimeException("Operador inválido: " + t);
                }
            }
        }

        if (stack.size() != 1)
            throw new RuntimeException("Erro ao avaliar expressão.");

        return stack.pop();
    }

    // =================== MAIN (REGISTRO RMI) ====================
    public static void main(String[] args)
            throws AccessException, RemoteException, AlreadyBoundException {

        Calculadora calculadora = new Calculadora();

        // Exporta o objeto remoto na porta 1100
        ICalculadora stub = (ICalculadora)
                UnicastRemoteObject.exportObject(calculadora, 1100);

        Registry reg;

        try {
            System.out.println("Criando registro RMI...");
            reg = LocateRegistry.createRegistry(1099);
        } catch (Exception e) {
            System.out.println("Registro já existe, conectando...");
            reg = LocateRegistry.getRegistry(1099);
        }

        // Nome "calculadora" no registro
        reg.rebind("calculadora", stub);

        System.out.println("Servidor Calculadora RMI pronto!");
    }
}