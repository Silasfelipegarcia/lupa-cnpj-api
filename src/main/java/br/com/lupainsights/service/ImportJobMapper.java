package br.com.lupainsights.service;

import br.com.lupainsights.dto.CnpjResult;
import br.com.lupainsights.dto.ImportJobStatus;
import br.com.lupainsights.dto.ImportRow;
import br.com.lupainsights.entity.ImportJobEntity;
import br.com.lupainsights.entity.ImportResultEntity;
import br.com.lupainsights.model.ImportJob;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ImportJobMapper {

    private final ObjectMapper objectMapper;

    public ImportJobMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ImportJob toDomain(ImportJobEntity entity, List<CnpjResult> resultados) {
        ImportJob job = new ImportJob(
                entity.getId(),
                entity.getUserId(),
                entity.getArquivo(),
                deserializarLinhas(entity.getLinhasJson()),
                entity.getCreatedAt()
        );
        job.setStatus(ImportJobStatus.valueOf(entity.getStatus()));
        job.setProcessados(entity.getProcessados());
        job.setSucesso(entity.getSucesso());
        job.setErros(entity.getErros());
        job.setMensagem(entity.getMensagem());
        job.setResultado(entity.getResultadoCsv());
        job.setConcluidoEm(entity.getCompletedAt());
        job.setCancelamentoSolicitado(entity.isCancelamentoSolicitado());
        job.substituirResultados(resultados);
        return job;
    }

    public ImportJobEntity toEntity(ImportJob job) {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(UUID.fromString(job.getId()));
        entity.setUserId(job.getUserId());
        entity.setArquivo(job.getArquivo());
        entity.setStatus(job.getStatus().name());
        entity.setTotal(job.getTotal());
        entity.setProcessados(job.getProcessados());
        entity.setSucesso(job.getSucesso());
        entity.setErros(job.getErros());
        entity.setMensagem(job.getMensagem());
        entity.setLinhasJson(serializarLinhas(job.getLinhas()));
        entity.setResultadoCsv(job.getResultado());
        entity.setCancelamentoSolicitado(job.isCancelamentoSolicitado());
        entity.setCreatedAt(job.getCriadoEm());
        entity.setCompletedAt(job.getConcluidoEm());
        return entity;
    }

    public List<ImportRow> deserializarLinhasPublico(String json) {
        return deserializarLinhas(json);
    }

    public ImportResultEntity toResultEntity(UUID jobId, int linhaNumero, CnpjResult result) {
        ImportResultEntity entity = new ImportResultEntity();
        entity.setJobId(jobId);
        entity.setLinhaNumero(linhaNumero);
        entity.setCnpj(result.getCnpj());
        entity.setRazaoSocialInformada(result.getRazaoSocialInformada());
        entity.setRazaoSocial(result.getRazaoSocial());
        entity.setNomeFantasia(result.getNomeFantasia());
        entity.setSituacaoCadastral(result.getSituacaoCadastral());
        entity.setTelefone1(result.getTelefone1());
        entity.setTelefone2(result.getTelefone2());
        entity.setEmail(result.getEmail());
        entity.setLogradouro(result.getLogradouro());
        entity.setNumero(result.getNumero());
        entity.setComplemento(result.getComplemento());
        entity.setBairro(result.getBairro());
        entity.setCidade(result.getCidade());
        entity.setUf(result.getUf());
        entity.setCep(result.getCep());
        entity.setCnaePrincipal(result.getCnaePrincipal());
        entity.setObservacao(result.getObservacao());
        entity.setStatusConsulta(result.getStatusConsulta());
        entity.setErro(result.getErro());
        return entity;
    }

    public CnpjResult toResultDto(ImportResultEntity entity) {
        CnpjResult result = new CnpjResult();
        result.setCnpj(entity.getCnpj());
        result.setRazaoSocialInformada(entity.getRazaoSocialInformada());
        result.setRazaoSocial(entity.getRazaoSocial());
        result.setNomeFantasia(entity.getNomeFantasia());
        result.setSituacaoCadastral(entity.getSituacaoCadastral());
        result.setTelefone1(entity.getTelefone1());
        result.setTelefone2(entity.getTelefone2());
        result.setEmail(entity.getEmail());
        result.setLogradouro(entity.getLogradouro());
        result.setNumero(entity.getNumero());
        result.setComplemento(entity.getComplemento());
        result.setBairro(entity.getBairro());
        result.setCidade(entity.getCidade());
        result.setUf(entity.getUf());
        result.setCep(entity.getCep());
        result.setCnaePrincipal(entity.getCnaePrincipal());
        result.setObservacao(entity.getObservacao());
        result.setStatusConsulta(entity.getStatusConsulta());
        result.setErro(entity.getErro());
        return result;
    }

    private String serializarLinhas(List<ImportRow> linhas) {
        try {
            return objectMapper.writeValueAsString(linhas);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar linhas da importação", e);
        }
    }

    private List<ImportRow> deserializarLinhas(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ImportRow>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao deserializar linhas da importação", e);
        }
    }
}
