package com.switchpay.channel.service;

import com.switchpay.channel.dto.ChannelRequest;
import com.switchpay.channel.dto.ChannelResponse;
import com.switchpay.channel.entity.Channel;
import com.switchpay.channel.entity.ChannelStatus;
import com.switchpay.channel.repository.ChannelRepository;
import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.exception.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final Clock clock;

    public ChannelService(ChannelRepository channelRepository, Clock clock) {
        this.channelRepository = channelRepository;
        this.clock = clock;
    }

    @Transactional
    public ChannelResponse create(ChannelRequest request) {
        if (channelRepository.existsByCode(request.code())) {
            throw new ConflictException("Channel code already exists: " + request.code());
        }

        Channel channel = new Channel();
        channel.setCode(request.code());
        channel.setType(request.type());
        channel.setName(request.name());
        channel.setLocation(request.location());
        channel.setStatus(request.status() != null ? request.status() : ChannelStatus.ACTIVE);
        channel.setCreatedAt(Instant.now(clock));

        return ChannelResponse.from(channelRepository.save(channel));
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> listAll() {
        return channelRepository.findAll().stream()
                .map(ChannelResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChannelResponse getById(Long id) {
        return ChannelResponse.from(findChannel(id));
    }

    @Transactional
    public ChannelResponse update(Long id, ChannelRequest request) {
        Channel channel = findChannel(id);

        boolean codeChanged = !channel.getCode().equals(request.code());
        if (codeChanged && channelRepository.existsByCode(request.code())) {
            throw new ConflictException("Channel code already exists: " + request.code());
        }

        channel.setCode(request.code());
        channel.setType(request.type());
        channel.setName(request.name());
        channel.setLocation(request.location());
        if (request.status() != null) {
            channel.setStatus(request.status());
        }
        return ChannelResponse.from(channel);
    }

    // soft delete: old transactions still point at this channel, so we only switch it off
    @Transactional
    public void deactivate(Long id) {
        Channel channel = findChannel(id);
        channel.setStatus(ChannelStatus.INACTIVE);
    }

    private Channel findChannel(Long id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Channel not found: " + id));
    }
}
