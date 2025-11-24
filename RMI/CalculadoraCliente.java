package RMI;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class CalculadoraCliente {

    public static void main(String[] args) {

        try {
            Registry reg = LocateRegistry.getRegistry(1099);
            ICalculadora calc = (ICalculadora) reg.lookup("calculadora");

            Scanner sc = new Scanner(System.in);

            System.out.println("Cliente RMI - Calculadora Remota");

            while (true) {
                System.out.println("\nMENU:");
                System.out.println("1 - Expressão (Abordagem 1 - cliente decompõe)");
                System.out.println("2 - Expressão (Abordagem 2 - servidor calcula)");
                System.out.println("3 - Sair");
                System.out.print("Escolha: ");

                String op = sc.nextLine();

                if (op.equals("3"))
                    break;

                System.out.print("Digite a expressão: ");
                String expr = sc.nextLine();

                try {
                    int resultado = 0;

                    if (op.equals("1")) {
                        System.out.println("Usando Abordagem 1 (cliente decompõe)");
                        resultado = avaliarNoCliente(expr, calc);
                    }
                    else if (op.equals("2")) {
                        System.out.println("Usando Abordagem 2 (servidor calcula)");
                        resultado = calc.calcularExpressao(expr);
                    }
                    else {
                        System.out.println("Opção inválida.");
                        continue;
                    }

                    System.out.println("Resultado: " + resultado);

                } catch (Exception e) {
                    System.out.println("Erro: " + e.getMessage());
                }
            }

            sc.close();

        } catch (Exception e) {
            System.out.println("Erro ao conectar ao RMI: " + e);
        }
    }

    // =================== Abordagem 1 ==========================

    /**
     * Converte a expressão usando shunting-yard e avalia RPN,
     * mas em vez de calcular localmente, chama os métodos remotos.
     */
    private static int avaliarNoCliente(String expr, ICalculadora calc)
            throws Exception {

        List<String> tokens = tokenize(expr);
        List<String> rpn = shuntingYard(tokens);

        Stack<Integer> stack = new Stack<>();

        for (String t : rpn) {
            if (t.matches("\\d+")) {
                stack.push(Integer.parseInt(t));
            } else {
                int b = stack.pop();
                int a = stack.pop();

                switch (t) {
                    case "+": stack.push(calc.soma(a,b)); break;
                    case "-": stack.push(calc.subtracao(a,b)); break;
                    case "*": stack.push(calc.multiplicacao(a,b)); break;
                    case "/": stack.push(calc.divisao(a,b)); break;
                    default: throw new RuntimeException("Operador inválido: " + t);
                }
            }
        }

        return stack.pop();
    }

    // **** Parsing reutilizado (mesma lógica do servidor) ****

    private static List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        s = s.replace(" ", "");
        int i=0;
        while (i<s.length()) {
            char c = s.charAt(i);
            if ("()+-*/".indexOf(c)>=0) {
                tokens.add(""+c);
                i++;
            } else if (Character.isDigit(c)) {
                StringBuilder n=new StringBuilder();
                while (i<s.length() && Character.isDigit(s.charAt(i))) {
                    n.append(s.charAt(i));
                    i++;
                }
                tokens.add(n.toString());
            } else {
                throw new RuntimeException("Caractere inválido: "+c);
            }
        }
        return tokens;
    }

    private static boolean isOp(String t) {
        return t.equals("+")||t.equals("-")||t.equals("*")||t.equals("/");
    }

    private static int prec(String op) {
        return (op.equals("+")||op.equals("-")) ? 1 : 2;
    }

    private static List<String> shuntingYard(List<String> tokens) {
        List<String> out=new ArrayList<>();
        Stack<String> st=new Stack<>();
        for (String t: tokens) {
            if (t.matches("\\d+")) out.add(t);
            else if (isOp(t)) {
                while (!st.isEmpty() && isOp(st.peek())
                       && prec(st.peek())>=prec(t)) {
                    out.add(st.pop());
                }
                st.push(t);
            }
            else if (t.equals("(")) st.push(t);
            else if (t.equals(")")) {
                while (!st.isEmpty() && !st.peek().equals("("))
                    out.add(st.pop());
                st.pop();
            }
        }
        while (!st.isEmpty()) out.add(st.pop());
        return out;
    }
}