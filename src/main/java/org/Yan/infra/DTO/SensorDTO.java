package org.Yan.infra.DTO;

/**
 * ipLocal: IP desta máquina na interface que alcança o sensor (obrigatório quando há vários adaptadores).
 * Ex.: sensor 192.168.2.5 na rede do adaptador 2 → ipLocal = IP do seu PC nesse adaptador (ex. 192.168.2.10).
 */
public class SensorDTO {
    private String ip;
    private int porta;
    private String nome;
    private String ipLocal;
    private String setor;

    public SensorDTO() {}

    public SensorDTO(String ip, int porta, String nome) {
        this.ip = ip;
        this.porta = porta;
        this.nome = nome;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPorta() {
        return porta;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    /** IP desta máquina na interface que alcança esse sensor (opcional; use quando tiver vários adaptadores). */
    public String getIpLocal() {
        return ipLocal;
    }

    public void setIpLocal(String ipLocal) {
        this.ipLocal = ipLocal;
    }
}
