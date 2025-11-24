package RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface da Calculadora Remota via RMI.
 * Define todas as operações que podem ser invocadas remotamente.
 */
public interface ICalculadora extends Remote {

    // Método já existente no esboço
    public int soma(int a, int b) throws RemoteException;

    /**
     * Calcula a subtração de dois inteiros remotamente.
     */
    public int subtracao(int a, int b) throws RemoteException;

    /**
     * Calcula a multiplicação de dois inteiros remotamente.
     */
    public int multiplicacao(int a, int b) throws RemoteException;

    /**
     * Calcula a divisão de dois inteiros remotamente.
     * Se b == 0, lança RemoteException.
     */
    public int divisao(int a, int b) throws RemoteException;

    /**
     * Abordagem 2:
     * Recebe uma expressão matemática como string e a calcula totalmente no servidor.
     * Exemplo: "(10 + 15) * 4"
     */
    public int calcularExpressao(String expressao) throws RemoteException;
}
