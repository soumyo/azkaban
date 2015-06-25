package azkaban.execapp.event;

import static azkaban.jobcallback.JobCallbackConstants.EXECUTION_ID_TOKEN;
import static azkaban.jobcallback.JobCallbackConstants.FLOW_TOKEN;
import static azkaban.jobcallback.JobCallbackConstants.HTTP_GET;
import static azkaban.jobcallback.JobCallbackConstants.HTTP_POST;
import static azkaban.jobcallback.JobCallbackConstants.JOB_STATUS_TOKEN;
import static azkaban.jobcallback.JobCallbackConstants.JOB_TOKEN;
import static azkaban.jobcallback.JobCallbackConstants.PROJECT_TOKEN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import azkaban.jobcallback.JobCallbackConstants;
import azkaban.jobcallback.JobCallbackStatusEnum;
import azkaban.utils.Props;

public class JobCallbackUtilTest {
  private static Map<String, String> contextInfo;

  private static final String PROJECT_NANE = "PROJECTX";
  private static final String FLOW_NANE = "FLOWX";
  private static final String JOB_NANE = "JOBX";
  private static final String EXECUTION_ID = "1234";

  @BeforeClass
  public static void setup() {
    contextInfo = new HashMap<String, String>();
    contextInfo.put(PROJECT_TOKEN, PROJECT_NANE);
    contextInfo.put(FLOW_TOKEN, FLOW_NANE);
    contextInfo.put(EXECUTION_ID_TOKEN, EXECUTION_ID);
    contextInfo.put(JOB_TOKEN, JOB_NANE);
    contextInfo.put(JOB_STATUS_TOKEN, JobCallbackStatusEnum.STARTED.name());
  }

  @Test
  public void noCallbackPropertiesTest() {
    Props props = new Props();
    props.put("abc", "def");

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.STARTED));

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.COMPLETED));

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.FAILURE));

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.SUCCESS));
  }

  @Test
  public void hasCallbackPropertiesTest() {
    Props props = new Props();
    for (JobCallbackStatusEnum jobStatus : JobCallbackStatusEnum.values()) {
      props.put(
          "job.notification." + jobStatus.name().toLowerCase() + ".1.url",
          "def");
    }

    System.out.println(props);

    Assert.assertTrue(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.STARTED));

    Assert.assertTrue(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.COMPLETED));

    Assert.assertTrue(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.FAILURE));

    Assert.assertTrue(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.SUCCESS));
  }

  @Test
  public void multipleStatusWithNoJobCallbackTest() {
    Props props = new Props();
    props.put("abc", "def");

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.STARTED, JobCallbackStatusEnum.COMPLETED,
        JobCallbackStatusEnum.FAILURE, JobCallbackStatusEnum.SUCCESS));

  }

  @Test
  public void multipleStatusesWithJobCallbackTest() {
    Props props = new Props();
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".1.url", "def");

    Assert.assertTrue(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.STARTED, JobCallbackStatusEnum.COMPLETED,
        JobCallbackStatusEnum.FAILURE, JobCallbackStatusEnum.SUCCESS));

    props = new Props();
    props.put("job.notification."
        + JobCallbackStatusEnum.COMPLETED.name().toLowerCase() + ".1.url",
        "def");
    Assert.assertTrue(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.STARTED, JobCallbackStatusEnum.COMPLETED,
        JobCallbackStatusEnum.FAILURE, JobCallbackStatusEnum.SUCCESS));

    props = new Props();
    props.put("job.notification."
        + JobCallbackStatusEnum.FAILURE.name().toLowerCase() + ".1.url", "def");
    Assert.assertTrue(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.STARTED, JobCallbackStatusEnum.COMPLETED,
        JobCallbackStatusEnum.FAILURE, JobCallbackStatusEnum.SUCCESS));

    props = new Props();
    props.put("job.notification."
        + JobCallbackStatusEnum.SUCCESS.name().toLowerCase() + ".1.url", "def");
    Assert.assertTrue(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.STARTED, JobCallbackStatusEnum.COMPLETED,
        JobCallbackStatusEnum.FAILURE, JobCallbackStatusEnum.SUCCESS));
  }

  @Test
  public void hasCallbackPropertiesWithGapTest() {
    Props props = new Props();
    for (JobCallbackStatusEnum jobStatus : JobCallbackStatusEnum.values()) {
      props.put(
          "job.notification." + jobStatus.name().toLowerCase() + ".2.url",
          "def");
    }

    System.out.println(props);

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.STARTED));

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.COMPLETED));

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.FAILURE));

    Assert.assertFalse(JobCallbackUtil.isThereJobCallbackProperty(props,
        JobCallbackStatusEnum.SUCCESS));
  }

  @Test
  public void noTokenTest() {
    String urlWithNoToken = "http://www.linkedin.com";
    String result = JobCallbackUtil.replaceToken(urlWithNoToken, contextInfo);
    Assert.assertEquals(urlWithNoToken, result);
  }

  @Test
  public void oneTokenTest() {

    String urlWithOneToken = "http://www.linkedin.com?project=" + PROJECT_TOKEN;

    String result = JobCallbackUtil.replaceToken(urlWithOneToken, contextInfo);
    Assert.assertEquals("http://www.linkedin.com?project=" + PROJECT_NANE,
        result);
  }

  @Test
  public void twoTokensTest() {

    String urlWithOneToken =
        "http://www.linkedin.com?project=" + PROJECT_TOKEN + "&flow="
            + FLOW_TOKEN;

    String result = JobCallbackUtil.replaceToken(urlWithOneToken, contextInfo);
    Assert.assertEquals("http://www.linkedin.com?project=" + PROJECT_NANE
        + "&flow=" + FLOW_NANE, result);
  }

  @Test
  public void parseJobCallbackOneGetTest() {
    Props props = new Props();
    String url = "http://lva1-rpt07.corp.linkedin.com";
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".1.url", url);
    List<HttpRequestBase> result =
        JobCallbackUtil.parseJobCallbackProperties(props,
            JobCallbackStatusEnum.STARTED, contextInfo, 3);

    Assert.assertEquals(1, result.size());
    Assert.assertEquals(HTTP_GET, result.get(0).getMethod());
    Assert.assertEquals(url, result.get(0).getURI().toString());
  }

  @Test
  public void parseJobCallbackWithInvalidURLTest() {
    Props props = new Props();
    String url = "linkedin.com";
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".1.url", url);
    List<HttpRequestBase> result =
        JobCallbackUtil.parseJobCallbackProperties(props,
            JobCallbackStatusEnum.STARTED, contextInfo, 3);

    Assert.assertEquals(1, result.size());
    Assert.assertEquals(HTTP_GET, result.get(0).getMethod());
    Assert.assertEquals(url, result.get(0).getURI().toString());
  }

  @Test
  public void parseJobCallbackTwoGetsTest() {
    Props props = new Props();
    String[] urls =
        { "http://lva1-rpt07.corp.linkedin.com",
            "http://lva1-rpt06.corp.linkedin.com" };
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".1.url",
        urls[0]);
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".2.url",
        urls[1]);
    List<HttpRequestBase> result =
        JobCallbackUtil.parseJobCallbackProperties(props,
            JobCallbackStatusEnum.STARTED, contextInfo, 3);

    Assert.assertEquals(2, result.size());
    for (int i = 0; i < urls.length; i++) {
      Assert.assertEquals(HTTP_GET, result.get(i).getMethod());
      Assert.assertEquals(urls[i], result.get(i).getURI().toString());
    }
  }

  @Test
  public void parseJobCallbackWithGapTest() {
    Props props = new Props();
    String[] urls =
        { "http://lva1-rpt07.corp.linkedin.com",
            "http://lva1-rpt06.corp.linkedin.com" };
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".1.url",
        urls[0]);
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".3.url",
        urls[1]);
    List<HttpRequestBase> result =
        JobCallbackUtil.parseJobCallbackProperties(props,
            JobCallbackStatusEnum.STARTED, contextInfo, 3);

    Assert.assertEquals(1, result.size());
    Assert.assertEquals(HTTP_GET, result.get(0).getMethod());
    Assert.assertEquals(urls[0], result.get(0).getURI().toString());
  }

  @Test
  public void parseJobCallbackWithPostTest() {
    Props props = new Props();
    String url = "http://lva1-rpt07.corp.linkedin.com";
    String bodyText = "{name:\"you\"}";
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".1.url", url);
    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".1.method",
        HTTP_POST);

    props.put("job.notification."
        + JobCallbackStatusEnum.STARTED.name().toLowerCase() + ".1.body",
        bodyText);

    List<HttpRequestBase> result =
        JobCallbackUtil.parseJobCallbackProperties(props,
            JobCallbackStatusEnum.STARTED, contextInfo, 3);

    Assert.assertEquals(1, result.size());

    HttpPost httpPost = (HttpPost) result.get(0);

    Assert.assertEquals(url, httpPost.getURI().toString());
    Assert.assertEquals(HTTP_POST, httpPost.getMethod());

    Assert.assertEquals(bodyText.length(), httpPost.getEntity()
        .getContentLength());

  }

  @Test
  public void noHeaderElementTest() {
    Header[] headerArr =
        JobCallbackUtil.parseHttpHeaders("this is an amazing day");

    Assert.assertNotNull(headerArr);
    Assert.assertEquals(0, headerArr.length);
  }

  @Test
  public void oneHeaderElementTest() {
    String name = "Content-type";
    String value = "application/json";
    String headers =
        name + JobCallbackConstants.HEADER_NAME_VALUE_DELIMITER + value;
    Header[] headerArr = JobCallbackUtil.parseHttpHeaders(headers);

    Assert.assertNotNull(headerArr);
    Assert.assertEquals(1, headerArr.length);
    Assert.assertEquals(name, headerArr[0].getName());
    Assert.assertEquals(value, headerArr[0].getValue());

    String headersWithExtraDelimiter =
        name + JobCallbackConstants.HEADER_NAME_VALUE_DELIMITER + value
            + JobCallbackConstants.HEADER_ELEMENT_DELIMITER;

    headerArr = JobCallbackUtil.parseHttpHeaders(headersWithExtraDelimiter);
    Assert.assertNotNull(headerArr);
    Assert.assertEquals(1, headerArr.length);
    Assert.assertEquals(name, headerArr[0].getName());
    Assert.assertEquals(value, headerArr[0].getValue());

  }

  @Test
  public void multipleHeaderElementTest() {
    String name1 = "Content-type";
    String value1 = "application/json";

    String name2 = "Accept";
    String value2 = "application/xml";

    String name3 = "User-Agent";
    String value3 =
        "Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0";

    String headers = makeHeaderElement(name1, value1);
    headers += JobCallbackConstants.HEADER_ELEMENT_DELIMITER;
    headers += makeHeaderElement(name2, value2);
    headers += JobCallbackConstants.HEADER_ELEMENT_DELIMITER;
    headers += makeHeaderElement(name3, value3);

    System.out.println("headers: " + headers);
    Header[] headerArr = JobCallbackUtil.parseHttpHeaders(headers);

    Assert.assertNotNull(headerArr);
    Assert.assertEquals(3, headerArr.length);
    Assert.assertEquals(name1, headerArr[0].getName());
    Assert.assertEquals(value1, headerArr[0].getValue());
    Assert.assertEquals(name2, headerArr[1].getName());
    Assert.assertEquals(value2, headerArr[1].getValue());
    Assert.assertEquals(name3, headerArr[2].getName());
    Assert.assertEquals(value3, headerArr[2].getValue());
  }

  @Test
  public void partialHeaderElementTest() {
    String name1 = "Content-type";
    String value1 = "application/json";

    String name2 = "Accept";
    String value2 = "";

    String name3 = "User-Agent";
    String value3 =
        "Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0";

    String headers = makeHeaderElement(name1, value1);
    headers += JobCallbackConstants.HEADER_ELEMENT_DELIMITER;
    headers += makeHeaderElement(name2, value2);
    headers += JobCallbackConstants.HEADER_ELEMENT_DELIMITER;
    headers += makeHeaderElement(name3, value3);

    System.out.println("headers: " + headers);
    Header[] headerArr = JobCallbackUtil.parseHttpHeaders(headers);

    Assert.assertNotNull(headerArr);
    Assert.assertEquals(3, headerArr.length);
    Assert.assertEquals(name1, headerArr[0].getName());
    Assert.assertEquals(value1, headerArr[0].getValue());
    Assert.assertEquals(name2, headerArr[1].getName());
    Assert.assertEquals(value2, headerArr[1].getValue());
    Assert.assertEquals(name3, headerArr[2].getName());
    Assert.assertEquals(value3, headerArr[2].getValue());
  }

  private String makeHeaderElement(String name, String value) {
    return name + JobCallbackConstants.HEADER_NAME_VALUE_DELIMITER + value;
  }

}
