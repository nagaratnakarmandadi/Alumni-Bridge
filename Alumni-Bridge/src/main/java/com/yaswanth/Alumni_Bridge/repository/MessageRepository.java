package com.yaswanth.Alumni_Bridge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.yaswanth.Alumni_Bridge.entity.Message;
import com.yaswanth.Alumni_Bridge.entity.User;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

	List<Message> findByReceiver(User receiver);

	List<Message> findBySenderOrReceiverOrderByTimestampDesc(User sender, User receiver);

	List<Message> findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(User sender, User receiver,
			User receiver2, User sender2);

	void deleteBySenderOrReceiver(User sender, User receiver);
}
