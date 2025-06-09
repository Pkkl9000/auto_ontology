package dpa.interop.auto_ontology.lmstudio_connect.dto;

import java.util.List;

public record ChatResponse(
        String id,
        String object,
        long created,
        String model,
        List<Choice> choices,
        Usage usage,
        Stats stats,
        ModelInfo model_info,
        Runtime runtime
) {
    public record Choice(
            int index,
            Object logprobs,
            String finish_reason,
            Message message
    ) {
        public record Message(
                String role,
                String content
        ) {}
    }

    public record Usage(
            int prompt_tokens,
            int completion_tokens,
            int total_tokens
    ) {}

    public record Stats(
            double tokens_per_second,
            double time_to_first_token,
            double generation_time,
            String stop_reason
    ) {}

    public record ModelInfo(
            String arch,
            String quant,
            String format,
            int context_length
    ) {}

    public record Runtime(
            String name,
            String version,
            List<String> supported_formats
    ) {}
}
