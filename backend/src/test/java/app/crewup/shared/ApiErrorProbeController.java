package app.crewup.shared;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints that exercise the shared API plumbing (errors, ETags, cursors) the way a module's controller
 * would, so its tests don't depend on a real feature. Hidden from OpenAPI, so they never reach the committed contract.
 */
@Hidden
@TestComponent
@RestController
@RequestMapping(ApiErrorProbeController.PATH)
public class ApiErrorProbeController {

    static final String PATH = "/api/test-probe/plumbing";
    static final long CURRENT_VERSION = 3;
    static final List<String> NAMES = List.of("Ada", "Bea", "Cy", "Di", "Ed");

    private final Cursors cursors;

    ApiErrorProbeController(Cursors cursors) {
        this.cursors = cursors;
    }

    record ProbeLink(@NotBlank String url) {}

    record ProbeRequest(
            @NotBlank String name, @Valid @Size(max = 2) List<ProbeLink> links) {}

    record NameKey(String name) {}

    @PostMapping("/validated")
    ResponseEntity<Void> validated(@Valid @RequestBody ProbeRequest request) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/not-found")
    void notFound() {
        throw new NotFoundException("No such thing");
    }

    @PutMapping("/versioned")
    ResponseEntity<Void> versioned(@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ETags.requireMatch(ifMatch, CURRENT_VERSION);
        return ResponseEntity.noContent()
                .header(HttpHeaders.ETAG, ETags.of(CURRENT_VERSION + 1))
                .build();
    }

    @GetMapping("/optimistic-lock")
    void optimisticLock() {
        throw new OptimisticLockingFailureException("Row was updated by another transaction");
    }

    @GetMapping("/names")
    CursorPage<String> names(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + Cursors.DEFAULT_LIMIT) @Min(1) @Max(Cursors.MAX_LIMIT) int limit) {
        String after = cursors.decode(cursor, NameKey.class).map(NameKey::name).orElse("");
        List<String> rows = NAMES.stream()
                .filter(name -> name.compareTo(after) > 0)
                .limit(limit + 1L)
                .toList();
        return cursors.page(rows, limit, NameKey::new);
    }
}
