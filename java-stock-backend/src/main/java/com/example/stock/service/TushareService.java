package com.example.stock.service;

import com.example.stock.util.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Tushare数据服务
 */
@Service
public class TushareService {

    @Value("${tushare.api.url:http://api.tushare.pro}")
    private String apiUrl;

    @Value("${tushare.token}")
    private String token;

    @Autowired
    private HttpUtil httpUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取A股日线行情
     *
     * @param tsCode    股票代码
     * @param startDate 开始日期 (yyyyMMdd)
     * @param endDate   结束日期 (yyyyMMdd)
     * @return 日线行情数据CSV格式
     * @throws IOException IO异常
     */
    public String getDaily(String tsCode, String startDate, String endDate) throws IOException {
        String stockType = getStockType(tsCode);
        String tsCodeNew = getTsCode(tsCode);

        // 构造请求参数
        String requestBody = "{\n" +
                "  \"api_name\": \"" + stockType + "\",\n" +
                "  \"token\": \"" + token + "\",\n" +
                "  \"params\": {\n" +
                "    \"ts_code\": \"" + tsCodeNew + "\",\n" +
                "    \"start_date\": \"" + startDate + "\",\n" +
                "    \"end_date\": \"" + endDate + "\"\n" +
                "  },\n" +
                "  \"fields\": \"ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount\"\n" +
                "}";

        String response = httpUtil.postJson(apiUrl, requestBody);

        // 解析响应
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode dataNode = rootNode.path("data");

        StringBuilder result = new StringBuilder();

        if (!dataNode.isMissingNode() && dataNode.has("fields") && dataNode.has("items")) {
            JsonNode fieldsNode = dataNode.path("fields");
            JsonNode itemsNode = dataNode.path("items");

            // 添加字段头
            StringBuilder header = new StringBuilder();
            for (JsonNode field : fieldsNode) {
                if (header.length() > 0) {
                    header.append(",");
                }
                header.append("\"").append(field.asText()).append("\"");
            }
            result.append(header).append("\n");

            // 添加数据行
            for (JsonNode item : itemsNode) {
                StringBuilder row = new StringBuilder();
                for (JsonNode value : item) {
                    if (row.length() > 0) {
                        row.append(",");
                    }
                    row.append("\"").append(value.asText()).append("\"");
                }
                result.append(row).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 根据股票代码获取对应的Tushare代码
     *
     * @param code 股票代码
     * @return Tushare代码
     */
    private String getTsCode(String code) {
        if (code.startsWith("US") || code.startsWith("us") || code.startsWith("gb_")) {
            return code.replace("gb_", "").replace("us", "");
        }
        return code;
    }

    /**
     * 根据股票代码获取对应的接口类型
     *
     * @param code 股票代码
     * @return 接口类型
     */
    private String getStockType(String code) {
        if (code.endsWith("SZ") || code.endsWith("SH") || code.endsWith("sh") || code.endsWith("sz")) {
            return "daily";
        }
        if (code.endsWith("HK") || code.endsWith("hk")) {
            return "hk_daily";
        }
        if (code.startsWith("US") || code.startsWith("us") || code.startsWith("gb_")) {
            return "us_daily";
        }
        return "";
    }
}