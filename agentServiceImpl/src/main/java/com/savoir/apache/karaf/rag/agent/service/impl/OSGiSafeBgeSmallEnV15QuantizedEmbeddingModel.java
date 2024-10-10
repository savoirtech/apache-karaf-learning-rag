package com.savoir.apache.karaf.rag.agent.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.embedding.TokenCountEstimator;
import dev.langchain4j.model.embedding.onnx.PoolingMode;

import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Includes code from:
 * https://github.com/langchain4j/langchain4j-embeddings/blob/0.35.0/langchain4j-embeddings/src/main/java/dev/langchain4j/model/embedding/onnx/AbstractInProcessEmbeddingModel.java
 *
 * Original Header below:
 */

/**
 * Quantized BAAI bge-small-en-v1.5 embedding model that runs within your Java application's process.
 * <p>
 * Maximum length of text (in tokens) that can be embedded at once: unlimited.
 * However, while you can embed very long texts, the quality of the embedding degrades as the text lengthens.
 * It is recommended to embed segments of no more than 512 tokens long.
 * <p>
 * Embedding dimensions: 384
 * <p>
 * It is recommended to add "Represent this sentence for searching relevant passages:" prefix to a query.
 * <p>
 * Uses an {@link Executor} to parallelize the embedding process.
 * By default, uses a cached thread pool with the number of threads equal to the number of available processors.
 * Threads are cached for 1 second.
 * <p>
 * More details <a href="https://huggingface.co/BAAI/bge-small-en-v1.5">here</a>
 */

public class OSGiSafeBgeSmallEnV15QuantizedEmbeddingModel extends DimensionAwareEmbeddingModel implements TokenCountEstimator {

    private Executor executor;

    //Replace loadFromJar with OSGI Safe methods...
    private static final OSGiSafeOnnxBertBiEncoder MODEL = loadFromBundle(
            "bge-small-en-v1.5-q.onnx",
            "bge-small-en-v1.5-q-tokenizer.json",
            PoolingMode.CLS
    );

    protected static OSGiSafeOnnxBertBiEncoder loadFromBundle(String modelFileName, String tokenizerFileName, PoolingMode poolingMode) {
        InputStream model = new java.io.BufferedInputStream(OSGiSafeBgeSmallEnV15QuantizedEmbeddingModel.class.getClassLoader().getResourceAsStream(modelFileName));
        InputStream tokenizer = new java.io.BufferedInputStream(OSGiSafeBgeSmallEnV15QuantizedEmbeddingModel.class.getClassLoader().getResourceAsStream(tokenizerFileName));
        return new OSGiSafeOnnxBertBiEncoder(model, tokenizer, poolingMode);
    }

    /**
     * Creates an instance of an {@code OSGiSafeBgeSmallEnV15QuantizedEmbeddingModel}.
     * Uses a cached thread pool with the number of threads equal to the number of available processors.
     * Threads are cached for 1 second.
     */
    public OSGiSafeBgeSmallEnV15QuantizedEmbeddingModel() {
        super();
        this.executor = getOrDefault(executor, this::createDefaultExecutor);
    }

    /**
     * Creates an instance of an {@code OSGiSafeBgeSmallEnV15QuantizedEmbeddingModel}.
     *
     * @param executor The executor to use to parallelize the embedding process.
     */
    public OSGiSafeBgeSmallEnV15QuantizedEmbeddingModel(Executor executor) {
        super();
    }

    protected OSGiSafeOnnxBertBiEncoder model() {
        return MODEL;
    }

    protected Integer knownDimension() {
        return 384;
    }

    //=================

    protected void OSGiSafeOnnxBertBiEncoder(Executor executor) {
        this.executor = (Executor) getOrDefault(executor, this::createDefaultExecutor);
    }

    private Executor createDefaultExecutor() {
        int threadPoolSize = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 1L, SECONDS, new LinkedBlockingQueue());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    static OSGiSafeOnnxBertBiEncoder loadFromFileSystem(Path pathToModel, Path pathToTokenizer, PoolingMode poolingMode) {
        try {
            return new OSGiSafeOnnxBertBiEncoder(Files.newInputStream(pathToModel), Files.newInputStream(pathToTokenizer), poolingMode);
        } catch (IOException var4) {
            IOException e = var4;
            throw new RuntimeException(e);
        }
    }

    static OSGiSafeOnnxBertBiEncoder loadFromFileSystem(Path pathToModel, InputStream tokenizer, PoolingMode poolingMode) {
        try {
            return new OSGiSafeOnnxBertBiEncoder(Files.newInputStream(pathToModel), tokenizer, poolingMode);
        } catch (IOException var4) {
            IOException e = var4;
            throw new RuntimeException(e);
        }
    }

    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        ValidationUtils.ensureNotEmpty(segments, "segments");
        return segments.size() == 1 ? this.embedInTheSameThread((TextSegment)segments.get(0)) : this.parallelizeEmbedding(segments);
    }

    private Response<List<Embedding>> embedInTheSameThread(TextSegment segment) {
        OSGiSafeOnnxBertBiEncoder.EmbeddingAndTokenCount embeddingAndTokenCount = this.model().embed(segment.text());
        return Response.from(Collections.singletonList(Embedding.from(embeddingAndTokenCount.embedding)), new TokenUsage(embeddingAndTokenCount.tokenCount - 2));
    }

    private Response<List<Embedding>> parallelizeEmbedding(List<TextSegment> segments) {
        List<CompletableFuture<OSGiSafeOnnxBertBiEncoder.EmbeddingAndTokenCount>> futures = (List)segments.stream().map((segment) -> {
            return CompletableFuture.supplyAsync(() -> {
                return this.model().embed(segment.text());
            }, this.executor);
        }).collect(Collectors.toList());
        int inputTokenCount = 0;
        List<Embedding> embeddings = new ArrayList();
        Iterator var5 = futures.iterator();

        while(var5.hasNext()) {
            CompletableFuture<OSGiSafeOnnxBertBiEncoder.EmbeddingAndTokenCount> future = (CompletableFuture)var5.next();

            try {
                OSGiSafeOnnxBertBiEncoder.EmbeddingAndTokenCount embeddingAndTokenCount = (OSGiSafeOnnxBertBiEncoder.EmbeddingAndTokenCount)future.get();
                embeddings.add(Embedding.from(embeddingAndTokenCount.embedding));
                inputTokenCount += embeddingAndTokenCount.tokenCount - 2;
            } catch (ExecutionException | InterruptedException var8) {
                Exception e = var8;
                throw new RuntimeException(e);
            }
        }

        return Response.from(embeddings, new TokenUsage(inputTokenCount));
    }

    public int estimateTokenCount(String text) {
        return this.model().countTokens(text);
    }

}
