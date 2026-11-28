package com.switchpay.channel.repository;

import com.switchpay.channel.entity.Channel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    boolean existsByCode(String code);

    Optional<Channel> findByCode(String code);
}
