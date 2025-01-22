package com.example.vm3.redis;

import com.example.vm3.entity.TbDtfHrasAuto;
import com.example.vm3.entity.TbDtfHrasAutoPk;
import com.example.vm3.repository.TbDtfHrasAutoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConsumer {

    private static final String STREAM_KEY = "dummyDataStream";
    private static final String GROUP_NAME = "group1";

    private final StringRedisTemplate redisTemplate;
    private final TbDtfHrasAutoRepository repository;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Value("${spring.application.consumer-name}")
    private String CONSUMER_NAME;

    @PersistenceContext
    private EntityManager em;


    @PostConstruct
    public void initializeConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            log.info("Consumer group already exists");
        }
    }

    public void consumeStream() {
        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();

        List<MapRecord<String, Object, Object>> messages = streamOps.read(
                Consumer.from(GROUP_NAME, CONSUMER_NAME), // consumer group & consumer name
                StreamReadOptions.empty().count(batchSize), // read option : batch size 설정
                StreamOffset.create(STREAM_KEY, ReadOffset.from(">"))  // steram offset (새로운 메시지부터 읽기 시작)
        );

        if (messages != null && !messages.isEmpty()) {
            int counter = 0;
            for (MapRecord<String, Object, Object> message : messages) {
                TbDtfHrasAuto entity = mapToEntity(message);
                repository.save(entity); // 개별 저장

                counter++;
                // 배치 크기마다 flush 및 clear 호출
                if (counter % batchSize == 0) {
                    repository.flush();
                    em.clear();
                }
            }

            // 남은 데이터 처리
            repository.flush();
            em.clear();
            log.info("Batch of {} messages saved to Oracle", messages.size());
        }
    }

    private TbDtfHrasAuto mapToEntity(MapRecord<String, Object, Object> message) {
        Map<Object, Object> value = message.getValue();

        TbDtfHrasAutoPk pk = new TbDtfHrasAutoPk(
                value.get("csId").toString(),
                LocalDateTime.parse(value.get("pdctDt").toString())
        );

        return TbDtfHrasAuto.builder()
                .pk(pk)
                .projectId(Long.parseLong(value.get("projectId").toString()))
                .name(value.get("name").toString())
                .riverName(value.get("riverName").toString())
                .riverReach(value.get("riverReach").toString())
                .riverCode(Long.parseLong(value.get("riverCode").toString()))
                .wtlvVal(Long.parseLong(value.get("wtlvVal").toString()))
                .flowVal(Long.parseLong(value.get("flowVal").toString()))
                .velVal(Long.parseLong(value.get("velVal").toString()))
                .build();
    }
}