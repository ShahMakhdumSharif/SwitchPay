package com.switchpay.channel.controller;

import com.switchpay.channel.dto.ChannelRequest;
import com.switchpay.channel.dto.ChannelResponse;
import com.switchpay.channel.service.ChannelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChannelResponse> create(@Valid @RequestBody ChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(channelService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ChannelResponse>> list() {
        return ResponseEntity.ok(channelService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChannelResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(channelService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChannelResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ChannelRequest request) {
        return ResponseEntity.ok(channelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        channelService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
