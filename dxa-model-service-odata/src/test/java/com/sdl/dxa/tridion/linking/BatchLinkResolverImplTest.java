package com.sdl.dxa.tridion.linking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.api.processors.LinkListProcessor;
import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;
import com.sdl.dxa.tridion.linking.descriptors.ComponentLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.RichTextLinkDescriptor;
import com.sdl.dxa.tridion.linking.impl.RichTextLinkResolverImpl;
import com.sdl.dxa.tridion.linking.processors.EntityLinkProcessor;
import com.sdl.dxa.tridion.linking.processors.FragmentLinkListProcessor;
import com.sdl.dxa.tridion.linking.processors.PageLinkProcessor;
import com.sdl.springconfig.BatchLinkResolverSpringConfig;
import com.sdl.web.api.linking.BatchLinkRequestImpl;
import com.sdl.web.api.linking.BatchLinkRetriever;
import com.sdl.web.api.linking.BatchLinkRetrieverImpl;
import com.sdl.web.api.linking.Link;
import com.sdl.web.linking.LinkImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_PAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = BatchLinkResolverSpringConfig.class)
public class BatchLinkResolverImplTest {
    private static final int COMPONENTLINKID = 123;

    @Autowired
    private BatchLinkResolver batchLinkResolver;

    @Autowired
    private BatchLinkResolver mockedBatchLinkResolver;

    @Autowired
    private BatchLinkRetriever mockedLinkRetriever;

    @Mock
    private ConfigService configService;

    @InjectMocks
    private RichTextLinkResolverImpl richTextLinkResolver;

    @Autowired
    ObjectMapper objectMapper;

    @Before
    public void init() {
        // Initialize ConfigService
        ConfigService.Defaults defaults = new ConfigService.Defaults(null, null);

        defaults.setRichTextXmlnsRemove(true);
        defaults.setRichTextResolve(true);

        Mockito.when(configService.getDefaults()).thenReturn(defaults);
    }

    @Test
    public void shouldDispatchLinkForResolution() {
        String pageId = "101";
        String entityId = "11";

        PageModelData page = mock(PageModelData.class);
        when(page.getId()).thenReturn(pageId);
        EntityModelData entity = mock(EntityModelData.class);
        when(entity.getId()).thenReturn(entityId);

        LinkProcessor pageLinkProcessor = new PageLinkProcessor(page);
        SingleLinkDescriptor pageLinkDescriptor = new ComponentLinkDescriptor(8, 18, COMPONENTLINKID, pageLinkProcessor, LINK_TYPE_PAGE);

        LinkProcessor entityLinkProcessor = new EntityLinkProcessor(entity);
        SingleLinkDescriptor componentLinkDescriptor = new ComponentLinkDescriptor(8, 18, COMPONENTLINKID, entityLinkProcessor, LINK_TYPE_COMPONENT);
        SingleLinkDescriptor binaryLinkDescriptor = new ComponentLinkDescriptor(8, 18, COMPONENTLINKID, entityLinkProcessor, LINK_TYPE_BINARY);

        this.batchLinkResolver.dispatchLinkResolution(pageLinkDescriptor);
        this.batchLinkResolver.dispatchLinkResolution(componentLinkDescriptor);
        this.batchLinkResolver.dispatchLinkResolution(binaryLinkDescriptor);

        this.batchLinkResolver.resolveAndFlush(null);

        assertNotNull(pageLinkDescriptor.getSubscription());
        assertNotNull(componentLinkDescriptor.getSubscription());
        assertNotNull(binaryLinkDescriptor.getSubscription());
    }

    @Test
    public void shouldDispatchMultipleLinkForResolutionWhenProcessingPageMeta() {
        BatchLinkRetriever retriever = mock(BatchLinkRetrieverImpl.class);
        BatchLinkResolverImpl resolver = new BatchLinkResolverImpl(true, true, true, retriever);

        Map<String, String> meta = new HashMap<String, String>(){
            { put("summary", "<p>Does life on Mars <a href=\"tcm:1-2\">exist</a>? Are we alone in<!--CompLink tcm:1-3--><a href=\"tcm:1-3\">space</a>?<!--CompLink tcm:1-3--></p>"); }
            {
                put(
                    "description",
                    "<p>Does life on Mars <a href=\"tcm:1-4\">exist</a>? Are we alone in<!--CompLink tcm:1-5--><a href=\"tcm:1-5\">space</a>?<!--CompLink tcm:1-5--></p>" +
                            "<p>Our scientists has found out that we are very possibly not <a href=\"tcm:1-6\">alone</a></p>"
            ); }
        };

        int count = 0;
        for (Map.Entry<String, String> entry : meta.entrySet()) {
            List<String> links = this.richTextLinkResolver.retrieveAllLinksFromFragment(entry.getValue());
            LinkListProcessor processor = new FragmentLinkListProcessor(
                    meta,
                    entry.getKey(),
                    entry.getValue(),
                    this.richTextLinkResolver
            );

            MultipleLinksDescriptor descriptor = new RichTextLinkDescriptor(1, 12, links, processor);

            resolver.dispatchMultipleLinksResolution(descriptor, new HashSet<>());
            count +=  descriptor.getLinks().size();
        }

        assertEquals(count, resolver.nrDescriptors());
    }

    @Test
    public void shouldNotDispatchDifferentRequestForEqualDescriptors() {
        PageModelData page = mock(PageModelData.class);
        when(page.getId()).thenReturn("101");


        LinkProcessor processor = new PageLinkProcessor(page);
        SingleLinkDescriptor descriptor_1 = new ComponentLinkDescriptor(8, 18, COMPONENTLINKID, processor, LINK_TYPE_PAGE);
        SingleLinkDescriptor descriptor_2 = new ComponentLinkDescriptor(8, 18, COMPONENTLINKID, processor, LINK_TYPE_PAGE);

        this.batchLinkResolver.dispatchLinkResolution(descriptor_1);
        this.batchLinkResolver.dispatchLinkResolution(descriptor_2);

        this.batchLinkResolver.resolveAndFlush(null);

        String subscriptionId_1 = descriptor_1.getSubscription();
        assertNotNull(subscriptionId_1);

        String subscriptionId_2 = descriptor_2.getSubscription();
        assertNotNull(subscriptionId_2);

        assertEquals(subscriptionId_1, subscriptionId_2);
    }

    @Test
    public void shouldBePossibleToChangeDescriptorType() {
        PageModelData page = mock(PageModelData.class);

        LinkProcessor processor = new PageLinkProcessor(page);
        SingleLinkDescriptor descriptor = new ComponentLinkDescriptor(8, 18, COMPONENTLINKID, processor, LINK_TYPE_PAGE);
        assertEquals(descriptor.getType(), LINK_TYPE_PAGE);

        descriptor.setType(LINK_TYPE_COMPONENT);
        assertEquals(descriptor.getType(), LINK_TYPE_COMPONENT);
    }

    @Test
    public void shouldResolveLink() {
        String subscriptionID = UUID.randomUUID().toString();
        String url = "/articles/simple-page/";

        PageModelData page = mock(PageModelData.class);
        when(page.getId()).thenReturn("101");
        when(page.setUrlPath(anyString())).thenCallRealMethod();

        Link resolvedLink = mock(LinkImpl.class);
        when(resolvedLink.getURL()).thenReturn(url);

        when(this.mockedLinkRetriever.addLinkRequest(any(BatchLinkRequestImpl.class))).thenReturn(subscriptionID);
        when(this.mockedLinkRetriever.getLink(subscriptionID)).thenReturn(resolvedLink);

        LinkProcessor processor = new PageLinkProcessor(page);
        SingleLinkDescriptor descriptor = new ComponentLinkDescriptor(8, 18, COMPONENTLINKID, processor, LINK_TYPE_PAGE);

        this.mockedBatchLinkResolver.dispatchLinkResolution(descriptor);
        this.mockedBatchLinkResolver.resolveAndFlush(null);

        assertEquals(LINK_TYPE_PAGE, descriptor.getType());
        assertEquals(url, descriptor.getResolvedLink());
    }

    @Test
    public void shouldStripIndexAnKeepTrailingSlashInResolveLink() {
        String subscriptionID = UUID.randomUUID().toString();
        String givenUrl = "/articles/simple-page/index.html";
        String expectedUrl = "/articles/simple-page/";

        PageModelData page = mock(PageModelData.class);
        when(page.getId()).thenReturn("101");

        Link resolvedLink = mock(LinkImpl.class);
        when(resolvedLink.getURL()).thenReturn(givenUrl);

        when(this.mockedLinkRetriever.addLinkRequest(any(BatchLinkRequestImpl.class))).thenReturn(subscriptionID);
        when(this.mockedLinkRetriever.getLink(subscriptionID)).thenReturn(resolvedLink);

        LinkProcessor processor = new PageLinkProcessor(page);
        SingleLinkDescriptor descriptor = new ComponentLinkDescriptor(8, 18, COMPONENTLINKID, processor, LINK_TYPE_PAGE);

        this.mockedBatchLinkResolver.dispatchLinkResolution(descriptor);
        this.mockedBatchLinkResolver.resolveAndFlush(null);

        assertEquals(LINK_TYPE_PAGE, descriptor.getType());
        assertEquals(expectedUrl, descriptor.getResolvedLink());
    }

}
