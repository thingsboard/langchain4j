package dev.langchain4j.model.googleai;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.concurrent.atomic.AtomicBoolean;

class CachedContentEvictingHandler implements StreamingChatResponseHandler {

    private final StreamingChatResponseHandler delegate;
    private final CachedContentSupplier cachedContentSupplier;
    private final Runnable retryAction;
    private final AtomicBoolean delivered = new AtomicBoolean();

    CachedContentEvictingHandler(StreamingChatResponseHandler delegate, CachedContentSupplier cachedContentSupplier, Runnable retryAction) {
        this.delegate = delegate;
        this.cachedContentSupplier = cachedContentSupplier;
        this.retryAction = retryAction;
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        delivered.set(true);
        delegate.onPartialResponse(partialResponse);
    }

    @Override
    public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
        delivered.set(true);
        delegate.onPartialResponse(partialResponse, context);
    }

    @Override
    public void onPartialThinking(PartialThinking partialThinking) {
        delivered.set(true);
        delegate.onPartialThinking(partialThinking);
    }

    @Override
    public void onPartialThinking(PartialThinking partialThinking, PartialThinkingContext context) {
        delivered.set(true);
        delegate.onPartialThinking(partialThinking, context);
    }

    @Override
    public void onPartialToolCall(PartialToolCall partialToolCall) {
        delivered.set(true);
        delegate.onPartialToolCall(partialToolCall);
    }

    @Override
    public void onPartialToolCall(PartialToolCall partialToolCall, PartialToolCallContext context) {
        delivered.set(true);
        delegate.onPartialToolCall(partialToolCall, context);
    }

    @Override
    public void onCompleteToolCall(CompleteToolCall completeToolCall) {
        delivered.set(true);
        delegate.onCompleteToolCall(completeToolCall);
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        delivered.set(true);
        delegate.onCompleteResponse(completeResponse);
    }

    @Override
    public void onError(Throwable error) {
        if (!delivered.get() && BaseGeminiChatModel.isCachedContentFailure(error)) {
            cachedContentSupplier.evict();
            retryAction.run();
        } else {
            delegate.onError(error);
        }
    }

}
