package org.Yan.service;

import org.Yan.driver.WirelessReader;
import org.Yan.exceptions.apiExceptions.TagListNotFoundException;
import org.Yan.exceptions.apiExceptions.TagNotFoundException;
import org.Yan.infra.DTO.TagDto;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SensorService implements ISensorService{
    private static final int CONNECTION_TIMEOUT_MS = 5000; // 5 segundos

    @Override
    public List<TagDto> GetAll(String ip, int port) {
        Set<String> tags = new HashSet<>();
        var response = new ArrayList<TagDto>();

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(10000); // Timeout de leitura: 10 segundos
            
            try {
                // Cada requisição cria sua própria instância de WirelessReader (thread-safe)
                WirelessReader sensor = new WirelessReader(socket);
                int hScanner = 0;
                tags = sensor.Net_getDistinctTags(hScanner);
                if(tags.isEmpty()){
                    throw new TagListNotFoundException(ip);
                }
                tags.forEach(tag -> {
                    response.add(new TagDto(tag, "Retifica Mecânica"));
                });
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }

        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Timeout ao conectar ao sensor em " + ip + ":" + port + 
                ". Verifique se o sensor está ligado e acessível.", e);
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Conexão recusada pelo sensor em " + ip + ":" + port + 
                ". Verifique se a porta está correta e o sensor está configurado para aceitar conexões TCP.", e);
        } catch (Exception e){
            throw new RuntimeException("Erro ao comunicar com o sensor em " + ip + ":" + port + 
                ". Detalhes: " + e.getMessage(), e);
        }
        return response;
    }

    @Override
    public TagDto GetById(String tagId, String ip, int port) {
        var findedTag = new TagDto("", "");
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(10000); // Timeout de leitura: 10 segundos
            
            try {
                // Cada requisição cria sua própria instância de WirelessReader (thread-safe)
                WirelessReader sensor = new WirelessReader(socket);
                int hScanner = 1;
                var tags = sensor.Net_getDistinctTags(hScanner);
                if(tags.isEmpty()){
                    throw new TagListNotFoundException(ip);
                }
                var tag = tags.stream().filter(t -> t.equalsIgnoreCase(tagId)).findFirst();
                if(tag.isEmpty()){
                     throw new TagNotFoundException(tagId);
                }
                findedTag = new TagDto(tag.get(),"Setor 01");
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Timeout ao conectar ao sensor em " + ip + ":" + port + 
                ". Verifique se o sensor está ligado e acessível.", e);
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Conexão recusada pelo sensor em " + ip + ":" + port + 
                ". Verifique se a porta está correta e o sensor está configurado para aceitar conexões TCP.", e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com o sensor em " + ip + ":" + port + 
                ". Detalhes: " + e.getMessage(), e);
        }
        return findedTag;
    }
    
    @Override
    public List<TagDto> GetAll(String ip, int port, String ipLocal) {
        return GetAll(ip, port);
    }
    
    @Override
    public TagDto GetById(String tagId, String ip, int port, String ipLocal) {
        return GetById(tagId, ip, port);
    }
}
