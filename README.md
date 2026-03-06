# KNTC Multi-Agent AI System

Hệ thống **Agentic AI + Multi-Agent** xử lý câu hỏi nghiệp vụ KNTC
(Khiếu nại - Tố cáo) với Spring Boot + OpenAI + Oracle.

---

## Kiến trúc

```
User Query
     ↓
[OrchestratorAgent]     LLM phân tích → lên kế hoạch (agent nào, thứ tự nào)
     ↓
  ┌──────────────────────────────────────────────────────┐
  │  Parallel / Sequential Execution (Topological Sort)  │
  │                                                       │
  │  [HoSoAgent]       GQ_HOSO + TN_HOSO                 │
  │  [LichAgent]       LICH_HOP                          │
  │  [ThongKeAgent]    Thống kê, báo cáo                 │
  │  [ThongBaoAgent]   THONG_BAO                         │
  └──────────────────────────────────────────────────────┘
     ↓
[SynthesisAgent]        Tổng hợp → câu trả lời hoàn chỉnh
```

---

## Agentic AI thực sự — ReAct Pattern

Mỗi Sub-Agent thực hiện vòng lặp **Reason → Act → Observe**:

```
Thought: Cần kiểm tra hồ sơ quá hạn và sắp đến hạn
Action: gq_find_overdue
Action Input: {"userId": 123}
Observation: [HS-001 quá hạn 2 ngày, HS-003 quá hạn 1 ngày]

Thought: Cần kiểm tra thêm hồ sơ sắp đến hạn trong 24h
Action: gq_find_due_soon
Action Input: {"userId": 123, "hours": 24}
Observation: [HS-005 đến hạn lúc 17:00 hôm nay]

Final Answer: Bạn có 3 hồ sơ cần ưu tiên xử lý gấp...
```

---

## Ví dụ câu hỏi → Agent routing

| Câu hỏi | Agents được gọi | Chạy |
|---------|----------------|------|
| "Hồ sơ gấp của tôi?" | HO_SO_AGENT | 1 agent |
| "Hôm nay tôi có gì?" | HO_SO + LICH + THONG_BAO | Song song |
| "Báo cáo tháng này?" | THONG_KE_AGENT | 1 agent |
| "Tìm hồ sơ Nguyễn Văn A" | HO_SO_AGENT | 1 agent |
| "Tóm tắt công việc + lịch tuần" | HO_SO + LICH | Song song |

---

## Tools của từng Agent

### HoSoAgent (GQ_HOSO + TN_HOSO)
| Tool | Mục đích |
|------|----------|
| `gq_find_assigned_today` | Hồ sơ được phân công hôm nay |
| `gq_find_overdue` | Hồ sơ quá hạn GQKNTC |
| `gq_find_due_soon` | Hồ sơ sắp đến hạn trong N giờ |
| `gq_find_overdue_ktrdk` | Hồ sơ quá hạn kiểm tra điều kiện |
| `gq_find_pending_signature` | Hồ sơ chờ trình ký |
| `gq_count` | Đếm hồ sơ theo filter |
| `gq_search` | Tìm kiếm theo mã/nội dung |
| `gq_find_need_xac_minh` | Hồ sơ cần tổ xác minh |
| `tn_find_pending` | Đơn thư chờ phân công |
| `tn_count_today` | Đơn thư tiếp nhận hôm nay |

### LichAgent
| Tool | Mục đích |
|------|----------|
| `lich_hop_hom_nay` | Cuộc họp hôm nay |
| `lich_sap_toi` | Lịch N ngày tới |
| `lich_count_tuan` | Số họp tuần này |

### ThongKeAgent
| Tool | Mục đích |
|------|----------|
| `tk_theo_trang_thai` | Thống kê theo trạng thái |
| `tk_gq_theo_thang` | Hồ sơ GQ theo tháng |
| `tk_tn_theo_thang` | Đơn TN theo tháng |
| `tk_qua_han` | Tổng hồ sơ quá hạn |
| `tk_tong_hop` | Thống kê tổng hợp |

### ThongBaoAgent
| Tool | Mục đích |
|------|----------|
| `tb_chua_doc` | Thông báo chưa đọc |
| `tb_count_unread` | Đếm thông báo chưa đọc |
| `tb_quan_trong` | Thông báo quan trọng |
| `tb_nhac_nho_hom_nay` | Nhắc nhở hôm nay |

---

## Cài đặt & Chạy

```bash
# 1. Environment variables
export ORACLE_HOST=localhost
export ORACLE_PORT=1521
export ORACLE_SID=ORCL
export ORACLE_USER=kntc_user
export ORACLE_PASSWORD=kntc_pass
export OPENAI_API_KEY=sk-...

# 2. Build & run
mvn spring-boot:run
```

---

## API

### POST /api/v1/kntc-agent/chat
```json
// Request
{
  "query": "Hôm nay tôi có hồ sơ gấp và lịch họp gì không?",
  "userId": 123,
  "userName": "Nguyễn Văn A"
}

// Response
{
  "answer": "📋 Kế hoạch hôm nay của bạn:\n\n🔴 Hồ sơ gấp...",
  "userId": 123,
  "query": "Hôm nay tôi có hồ sơ gấp và lịch họp gì không?",
  "processingTimeMs": 3420,
  "timestamp": "2026-03-05T09:00:00"
}
```

---

## Mở rộng — Thêm Agent mới

Chỉ cần 3 bước:

```java
// 1. Tạo class extends BaseReActAgent
@Component
public class VanBanAgent extends BaseReActAgent {
    @Override public String getAgentId() { return "VAN_BAN_AGENT"; }
    @Override public String getDomain()  { return "Văn bản"; }
    @Override protected String buildSystemPrompt() { return "..."; }
    @Override protected String executeTool(String tool, JsonNode args) { ... }
}

// 2. Cập nhật ORCHESTRATOR_PROMPT thêm dòng:
// - VAN_BAN_AGENT : văn bản đi/đến, công văn

// 3. Done! Orchestrator tự nhận diện và routing
```

---

## Cấu trúc project

```
src/main/java/vn/vnpt/kntc/
├── agent/
│   ├── base/
│   │   ├── BaseReActAgent.java      ← ReAct loop engine
│   │   ├── AgentContext.java
│   │   └── AgentResult.java
│   └── impl/
│       ├── HoSoAgent.java           ← GQ_HOSO + TN_HOSO
│       └── OtherAgents.java         ← Lich, ThongBao, ThongKe
├── workflow/
│   ├── OrchestratorAgent.java       ← Điều phối + parallel exec
│   ├── OrchestrationPlan.java
│   └── SynthesisAgent.java          ← Tổng hợp kết quả
├── model/          ← Entities (GqHoSo, ...)
├── repository/     ← JPA Repositories
├── service/        ← KntcAgentService
├── controller/     ← REST API
└── config/
```
