package com.pedidos360.kafka.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO con informacion del estado de un Consumer Group en Kafka.
 */
public class ConsumerGroupInfoDto {

    private String groupId;
    private String state;
    private String protocolType;
    private String coordinator;
    private int membersCount;
    private List<MemberDetailDto> members;
    private Map<String, Long> lagByPartition;
    private long totalLag;

    public ConsumerGroupInfoDto() {
    }

    public ConsumerGroupInfoDto(String groupId, String state, String protocolType, String coordinator,
                                int membersCount, List<MemberDetailDto> members,
                                Map<String, Long> lagByPartition, long totalLag) {
        this.groupId = groupId;
        this.state = state;
        this.protocolType = protocolType;
        this.coordinator = coordinator;
        this.membersCount = membersCount;
        this.members = members;
        this.lagByPartition = lagByPartition;
        this.totalLag = totalLag;
    }

    public static class MemberDetailDto {
        private String memberId;
        private String clientId;
        private String host;
        private List<String> assignedPartitions;

        public MemberDetailDto() {
        }

        public MemberDetailDto(String memberId, String clientId, String host, List<String> assignedPartitions) {
            this.memberId = memberId;
            this.clientId = clientId;
            this.host = host;
            this.assignedPartitions = assignedPartitions;
        }

        public String getMemberId() {
            return memberId;
        }

        public void setMemberId(String memberId) {
            this.memberId = memberId;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public List<String> getAssignedPartitions() {
            return assignedPartitions;
        }

        public void setAssignedPartitions(List<String> assignedPartitions) {
            this.assignedPartitions = assignedPartitions;
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(String coordinator) {
        this.coordinator = coordinator;
    }

    public int getMembersCount() {
        return membersCount;
    }

    public void setMembersCount(int membersCount) {
        this.membersCount = membersCount;
    }

    public List<MemberDetailDto> getMembers() {
        return members;
    }

    public void setMembers(List<MemberDetailDto> members) {
        this.members = members;
    }

    public Map<String, Long> getLagByPartition() {
        return lagByPartition;
    }

    public void setLagByPartition(Map<String, Long> lagByPartition) {
        this.lagByPartition = lagByPartition;
    }

    public long getTotalLag() {
        return totalLag;
    }

    public void setTotalLag(long totalLag) {
        this.totalLag = totalLag;
    }
}
