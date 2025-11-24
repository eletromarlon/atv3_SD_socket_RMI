package Socket;
import java.util.*;
/**
 * Olha eu voltando a escrever Java em 2025...
 * Calculadora: operações básicas + avaliador de expressões.
 * - Métodos: soma, subtracao, multiplicacao, divisao (com tratamento de divisão por zero)
 * - Método calcularExpressao(String): converte infixa -> RPN (Shunting-yard) e avalia RPN.
 */
public class Calculadora {

    public String sayHello(String nome, String sobrenome) {
        return "Fala " + nome + " " + sobrenome;
    }

    public double soma(double oper1, double oper2) { return oper1 + oper2; }
    // A partir daqui, foram adicionados os outros métodos por Marlon Duarte
    public double subtracao(double oper1, double oper2) { return oper1 - oper2; }
    public double multiplicacao(double oper1, double oper2) { return oper1 * oper2; }
    // Tratamento de divisão por zero
    public double divisao(double oper1, double oper2) {
        if (oper2 == 0.0) {
            throw new ArithmeticException("Divisao por zero");
        }
        return oper1 / oper2;
    }

    /**
     * Calcula expressão completa dada em notação infixa.
     * Implementação:
     * 1) Tokeniza a expressão (números, operadores, parênteses)
     * 2) Converte para RPN via algoritmo Shunting-yard
     * 3) Avalia RPN localmente
     * Retorna resultado como String (ou mensagem de erro).
     */
    public String calcularExpressao(String expressao) {
        try {
            List<String> tokens = tokenize(expressao);
            List<String> rpn = shuntingYard(tokens);
            double result = evalRPN(rpn);
            // Remover .0 desnecessário
            if (result == (long) result) {
                return String.format("%d", (long) result);
            } else {
                return String.valueOf(result);
            }
        } catch (ArithmeticException ae) {
            return "ERRO: " + ae.getMessage();
        } catch (Exception e) {
            return "ERRO: expressao invalida (" + e.getMessage() + ")";
        }
    }

    // ===== Tokenização =====
    private List<String> tokenize(String s) {
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
                // Pode ser operador binário ou sinal de número (unário)
                // Se no início ou após '(' ou outro operador => faz parte do número
                if (tokens.isEmpty() || isOperator(tokens.get(tokens.size()-1)) || tokens.get(tokens.size()-1).equals("(")) {
                    // ler número negativo
                    StringBuilder sb = new StringBuilder();
                    sb.append('-');
                    i++;
                    // ler dígitos/decimal
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

    private boolean isOperator(String tok) {
        return tok.equals("+") || tok.equals("-") || tok.equals("*") || tok.equals("/");
    }

    // ===== Shunting-yard: infix -> RPN =====
    private List<String> shuntingYard(List<String> tokens) {
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
                stack.pop(); // remove "("
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

    private boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) { return false; }
    }

    private int prec(String op) {
        switch (op) {
            case "+": case "-": return 1;
            case "*": case "/": return 2;
        }
        return 0;
    }

    private boolean isLeftAssoc(String op) {
        // + - * / all left-associative
        return true;
    }

    // ===== Avalia RPN localmente =====
    private double evalRPN(List<String> rpn) {
        Deque<Double> stack = new ArrayDeque<>();
        for (String tok : rpn) {
            if (isNumber(tok)) {
                stack.push(Double.parseDouble(tok));
            } else if (isOperator(tok)) {
                if (stack.size() < 2) throw new IllegalArgumentException("RPN invalido");
                double b = stack.pop();
                double a = stack.pop();
                double res;
                switch (tok) {
                    case "+": res = soma(a,b); break;
                    case "-": res = subtracao(a,b); break;
                    case "*": res = multiplicacao(a,b); break;
                    case "/": res = divisao(a,b); break;
                    default: throw new IllegalArgumentException("Operador desconhecido: " + tok);
                }
                stack.push(res);
            } else {
                throw new IllegalArgumentException("Token RPN invalido: " + tok);
            }
        }
        if (stack.size() != 1) throw new IllegalArgumentException("RPN produziu mais de um valor");
        return stack.pop();
    }
}
