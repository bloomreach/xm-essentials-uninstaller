package org.onehippo.cms7.essentials.plugins.uninstaller;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.*;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.parser.DefaultInstructionParser;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;
import org.onehippo.cms7.essentials.plugins.uninstaller.model.SimpleNode;
import org.onehippo.cms7.essentials.plugins.uninstaller.model.UninstallModel;
import org.onehippo.cms7.essentials.plugins.uninstaller.service.PluginService;
import org.onehippo.cms7.essentials.plugins.uninstaller.service.PluginSet;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.rest.InstallState;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("uninstaller")
public class UnInstallResource {

    private static final Logger log = LoggerFactory.getLogger(UnInstallResource.class);

    @Inject
    private JcrService jcrService;
    @Inject
    private ContentTypeService contentTypeService;
    @Inject
    private SettingsService settingsService;
    @Inject
    private ContextXmlService contextXmlService;
    @Inject
    private LoggingService loggingService;
    @Inject
    private MavenAssemblyService mavenAssemblyService;
    @Inject
    private MavenCargoService mavenCargoService;
    @Inject
    private MavenDependencyService mavenDependencyService;
    @Inject
    private MavenModelService mavenModelService;
    @Inject
    private MavenRepositoryService mavenRepositoryService;
    @Inject
    private PlaceholderService placeholderService;
    @Inject
    private ProjectService projectService;
    @Inject
    private RebuildService rebuildService;
    @Inject
    private TemplateQueryService templateQueryService;
    @Inject
    private WebXmlService webXmlService;

    @Inject
    private DefaultInstructionParser parser;

    @GET
    @Path("/plugin/{plugin}/info")
    public UninstallModel getUninstallModel(@PathParam("plugin") String plugin) throws IOException {
        List<PluginDescriptor> uninstallablePlugins = getAllUninstallable();

        Optional<PluginDescriptor> optionalPluginDescriptor = Optional.ofNullable(uninstallablePlugins.stream().filter(pluginDescriptor -> pluginDescriptor.getId().equals(plugin)).findFirst()
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND)));

        PluginDescriptor uninstallablePlugin = optionalPluginDescriptor.get();

        String packageFile = uninstallablePlugin.getPackageFile();
        InputStream resourceAsStream = getClass().getResourceAsStream(packageFile);

        PluginInstructions pluginInstructions = parser.parseInstructions(IOUtils.toString(resourceAsStream));

        List<Instruction> instructions = pluginInstructions.getInstructionSets().stream().flatMap(pluginInstructionSet -> pluginInstructionSet.getInstructions().stream()).filter(instruction -> instruction instanceof BuiltinInstruction).collect(Collectors.toList());

        Map<String, Object> parameters = placeholderService.makePlaceholders();

        Map<String, Object> parsedParameters = parameters.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().toString()));

        UninstallModel uninstallModel = new UninstallModel();
        uninstallModel.setParameters(parsedParameters);
        uninstallModel.setInstructions(instructions);

        return uninstallModel;
    }

    @GET
    @Path("/plugin/all")
    public List<PluginDescriptor> getAllUninstallable() throws IOException {
        List<Object> providers = new ArrayList<>();
        providers.add(new JAXBElementProvider());
        providers.add(new JacksonJsonProvider());

        PluginService service = JAXRSClientFactory.create(
                "http://localhost:8080/essentials/rest/",
                PluginService.class,
                providers);

        WebClient.client(service)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE);

        List<PluginDescriptor> allPlugins = service.getPlugins().stream().filter(pluginDescriptor -> !pluginDescriptor.getId().equals("bloomreachInstallAllPlugins")).collect(Collectors.toList());

        PluginSet allPluginSet = new PluginSet();
        allPlugins.stream()
                .forEach(allPluginSet::add);

        Set<PluginDescriptor> inheritedPlugins = new HashSet<>();
        allPlugins.stream().forEach(pluginDescriptor -> Optional.ofNullable(
                pluginDescriptor.getPluginDependencies())
                .ifPresent(
                        dependencies -> inheritedPlugins
                                .addAll(dependencies.stream()
                                        .map(dependency -> allPluginSet.getPlugin(dependency.getPluginId()))
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList()))));

        List<PluginDescriptor> installedPlugins = allPlugins.stream()
                .filter(pluginDescriptor -> Arrays.asList(InstallState.INSTALLED, InstallState.INSTALLING, InstallState.INSTALLATION_PENDING).contains(pluginDescriptor.getState()))
                .collect(Collectors.toList());

        List<PluginDescriptor> uninstallablePlugins = new ArrayList<>(installedPlugins);
        uninstallablePlugins.removeAll(inheritedPlugins);

        uninstallablePlugins = uninstallablePlugins.stream()
                .filter(pluginDescriptor -> pluginDescriptor.getType().equals("feature"))
                .filter(pluginDescriptor -> pluginDescriptor.getPackageFile() != null)
                .collect(Collectors.toList());

        return uninstallablePlugins;
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @GET
    @Path("/plugin/{plugin}/uninstall")
    public void uninstall(@PathParam("plugin") String plugin) throws IOException {
        UninstallModel uninstallModel = getUninstallModel(plugin);
        List<Instruction> instructions = uninstallModel.getInstructions();

        instructions.forEach(instruction -> {

            try {
                InstructionType type = InstructionType.valueOf(instruction.getClass().getSimpleName());
                switch (type) {
                    case MavenDependencyInstruction:
                        MavenDependencyInstruction mavenDependencyInstruction = InstructionType.getInstruction(instruction);
                        String targetPom = mavenDependencyInstruction.getTargetPom();
                        String pluginLocation = "/META-INF/maven." + mavenDependencyInstruction.getGroupId() + "." + mavenDependencyInstruction.getArtifactId() + "/pom.xml";
                        URL resource = getClass().getResource(pluginLocation);
                        break;
                    case XmlInstruction:
                        XmlInstruction xmlInstruction = InstructionType.getInstruction(instruction);
                        final String xmlTarget = TemplateUtils.replaceTemplateData(xmlInstruction.getTarget(), uninstallModel.getParameters());
                        String nodeName = getName(xmlInstruction.getSource());
                        removeNode(xmlTarget, nodeName);
                        break;
                    case FileInstruction:
                        FileInstruction fileInstruction = InstructionType.getInstruction(instruction);
                        final String fileTarget = TemplateUtils.replaceTemplateData(fileInstruction.getTarget(), uninstallModel.getParameters());
                        removeFile(fileTarget);
                        break;
                    case FreemarkerInstruction:
                        FileInstruction freeMarkerInstruction = InstructionType.getInstruction(instruction);
                        final String freeMarkerTarget = TemplateUtils.replaceTemplateData(freeMarkerInstruction.getTarget(), uninstallModel.getParameters());
                        removeFile(freeMarkerTarget);
                        break;
                    default:
                        break;
                }
            } catch (IllegalArgumentException e) {

            }


        });

        String essentialsRoot = (String) uninstallModel.getParameters().get("essentialsRoot");
        String pluginPath = Paths.get(essentialsRoot, "src/main/resources", plugin + ".xml").toString();
        removeFile(pluginPath);
    }


    private List<FileObject> getAllFiles(FileObject fileObject) {
        return new FileObjectFlattener(fileObject).flattened().map(FileObjectFlattener::getFileObject).collect(Collectors.toList());
    }

    public void removeFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeNode(String path, String name) {
        Session session = jcrService.createSession();
        try {
            session.getNode(path).getNode(name).remove();
            session.save();
        } catch (RepositoryException e) {
            e.printStackTrace();
        } finally {
            session.logout();
        }
    }

    public String getName(String source) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(SimpleNode.class);
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            SimpleNode simpleNode = (SimpleNode) unmarshaller.unmarshal(getClass().getResourceAsStream("/" + source));
            return simpleNode.getName();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }

    public enum InstructionType {
        MavenDependencyInstruction(MavenDependencyInstruction.class.getSimpleName()),
        XmlInstruction(XmlInstruction.class.getSimpleName()),
        FileInstruction(FileInstruction.class.getSimpleName()),
        FreemarkerInstruction(FreemarkerInstruction.class.getSimpleName())
//        CndInstruction(CndInstruction.class.getSimpleName()),
        ;

        private String simpleClassName;

        InstructionType(String simpleClassName) {
            this.simpleClassName = simpleClassName;
        }

        public static <T> T getInstruction(Instruction instruction) {
            return (T) instruction;
        }
    }
}


