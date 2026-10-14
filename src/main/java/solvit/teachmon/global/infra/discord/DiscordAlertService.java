package solvit.teachmon.global.infra.discord;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.global.exception.ErrorResponse;
import solvit.teachmon.global.properties.DiscordProperties;
import solvit.teachmon.global.security.user.TeachmonUserDetails;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DiscordAlertService {
    private final RestClient restClient;
    private final DiscordProperties discordProperties;

    public void alertError(HttpServletRequest request, ErrorResponse errorResponse) {
        Map<String, Object> payload = prepareDiscordPayload(request, errorResponse);

        restClient.post()
                .uri(discordProperties.getWebhook())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    // 스케줄러(배치)는 요청이 없어 GlobalExceptionHandler를 거치지 않으므로 직접 알린다
    public void alertBatchFailure(String jobName, LocalDate baseDate, Throwable throwable) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "🚨 배치 실패");
        embed.put("color", 16711680);
        embed.put("fields", Arrays.asList(
                field("발생시각", LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString(), false),
                field("배치", jobName, false),
                field("기준일", String.valueOf(baseDate), true),
                field("예외", throwable.getClass().getName(), false),
                field("메시지", limit(throwable.getMessage()), false)
        ));

        restClient.post()
                .uri(discordProperties.getWebhook())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("embeds", List.of(embed)))
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> prepareDiscordPayload(HttpServletRequest request, ErrorResponse errorResponse) {
        RequesterInfo requester = resolveRequester();

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "🚨 서버 에러 발생");
        embed.put("color", 16711680);

        embed.put("fields", Arrays.asList(
                field("발생시각", LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString(), false),
                field("요청자", requester.toMap().toString(), false),
                field("요청 URI", request.getRequestURI(), false),
                field("HTTP 메서드", request.getMethod(), true),
                field("응답 상태코드", String.valueOf(errorResponse.getStatus()), true),
                field("쿼리스트링", Optional.ofNullable(request.getQueryString()).orElse("존재하지 않음"), false),
                field("요청 파라미터", extractRequestParams(request), false),
                field("요청 본문", limit(extractRequestBody(request)), false),
                field("응답 본문", limit(errorResponse.getMessage()), false)
        ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("embeds", List.of(embed));

        return payload;
    }

    private Map<String, Object> field(String name, String value, boolean inline) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("value", value == null || value.isBlank() ? "없음" : value);
        map.put("inline", inline);
        return map;
    }

    private String limit(String value) {
        if (value == null) return "없음";
        return value.length() > 1000 ? value.substring(0, 1000) + "...(생략)" : value;
    }

    private RequesterInfo resolveRequester() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return new RequesterInfo("guest", "guest", "guest");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof TeachmonUserDetails) {
            TeacherEntity teacher = ((TeachmonUserDetails) principal).teacherEntity();
            return new RequesterInfo(teacher.getName(), teacher.getMail(), teacher.getRole().getValue());
        }

        return new RequesterInfo(authentication.getName(), "unknown", joinAuthorities(authentication));
    }

    private String extractRequestParams(HttpServletRequest request) {
        if (request.getParameterMap().isEmpty()) {
            return "존재하지 않음";
        }

        return request.getParameterMap()
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + joinValues(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private String extractRequestBody(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper == null) {
            return "존재하지 않음";
        }

        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return "존재하지 않음";
        }

        Charset charset = Optional.of(wrapper.getCharacterEncoding())
                .map(Charset::forName)
                .orElse(StandardCharsets.UTF_8);
        return new String(content, charset);
    }

    private String joinAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private String joinValues(String[] values) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        Arrays.stream(values).forEach(joiner::add);
        return joiner.toString();
    }

    private record RequesterInfo(String name, String email, String role) {
        Map<String, String> toMap() {
            return Map.of(
                    "이름", name,
                    "이메일", email,
                    "역할", role
            );
        }
    }
}
