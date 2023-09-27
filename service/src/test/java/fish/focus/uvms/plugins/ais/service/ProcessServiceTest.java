package fish.focus.uvms.plugins.ais.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.ais.Sentence;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.plugins.ais.StartupBean;
import fish.focus.uvms.plugins.ais.service.DownsamplingService;
import fish.focus.uvms.plugins.ais.service.ExchangeService;
import fish.focus.uvms.plugins.ais.service.ProcessResult;
import fish.focus.uvms.plugins.ais.service.ProcessService;

/*
 * Test data taken from https://fossies.org/linux/gpsd/test/sample.aivdm
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessServiceTest {

    @Mock
    private StartupBean startUp;

    @Mock
    private DownsamplingService downsamplingService;
    
    @Mock
    private ExchangeService exchangeService;
    
    @InjectMocks
    private ProcessService processService;
    
    @Captor
    private ArgumentCaptor<List<MovementBaseType>> captor;


    @Before
    public void init(){
        //onlyFishingVessels should be off for all test
        when(startUp.getSetting("onlyFishingVessels")).thenReturn("false");
    }
    @Test
    public void aisType1Test() {
        ProcessResult result = processService.processMessages(Arrays.asList(getAisType1Message()), new HashSet<>());
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        MovementBaseType movement = movements.get("371798000");
        assertThat(movement.getMmsi(), is("371798000"));
        assertThat(movement.getFlagState(), is("PAN"));
        assertThat(movement.getPosition().getLatitude(), is(48.38163333333333));
        assertThat(movement.getPosition().getLongitude(), is(-123.39538333333333));
        assertThat(movement.getTrueHeading(), is(215));
        assertThat(movement.getReportedSpeed(), is(12.3));
        assertThat(movement.getAisPositionAccuracy(), is((short)1));
        int positionSecond = movement.getPositionTime().toInstant().atZone(ZoneOffset.UTC).getSecond();
        assertThat(positionSecond, is(33));
    }

    @Test
    public void aisType1WithCommentBlockTest() {
        Sentence aisType1MessageWithCommentBlock = new Sentence("1G1:32,s:516,c:1652227200*5B", getAisType1Message().getSentence());
        ProcessResult result = processService.processMessages(Arrays.asList(aisType1MessageWithCommentBlock), new HashSet<>());
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        MovementBaseType movement = movements.get("371798000");
        assertThat(movement.getMmsi(), is("371798000"));
        assertThat(movement.getFlagState(), is("PAN"));
        assertThat(movement.getPosition().getLatitude(), is(48.38163333333333));
        assertThat(movement.getPosition().getLongitude(), is(-123.39538333333333));
        assertThat(movement.getTrueHeading(), is(215));
        assertThat(movement.getReportedSpeed(), is(12.3));
        assertThat(movement.getAisPositionAccuracy(), is((short) 1));

        ZonedDateTime timestamp = movement.getPositionTime().toInstant().atZone(ZoneOffset.UTC);
        int positionSecond = timestamp.getSecond();
        assertThat(positionSecond, is(33));
        assertThat(timestamp, is(Instant.ofEpochSecond(1652227200).minus(1, ChronoUnit.MINUTES).atZone(ZoneOffset.UTC).withSecond(33)));
    }

    @Test
    public void aisType2Test() {
        ProcessResult result = processService.processMessages(Arrays.asList(getAisType2Message()), new HashSet<>());
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        MovementBaseType movement = movements.get("356302000");
        assertThat(movement.getMmsi(), is("356302000"));
        assertThat(movement.getFlagState(), is("PAN"));
        assertThat(movement.getPosition().getLatitude(), is(40.39235833333333));
        assertThat(movement.getPosition().getLongitude(), is(-71.62614333333333333333333333));
        assertThat(movement.getTrueHeading(), is(91));
        assertThat(movement.getReportedSpeed(), is(13.9));
        assertThat(movement.getAisPositionAccuracy(), is((short)0));
        int positionSecond = movement.getPositionTime().toInstant().atZone(ZoneOffset.UTC).getSecond();
        assertThat(positionSecond, is(41));
    }
    
    @Test
    public void aisType3Test() {
        ProcessResult result = processService.processMessages(Arrays.asList(getAisType3Message()), new HashSet<>());
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        MovementBaseType movement = movements.get("563808000");
        assertThat(movement.getMmsi(), is("563808000"));
        assertThat(movement.getFlagState(), is("SGP"));
        assertThat(movement.getPosition().getLatitude(), is(36.91));
        assertThat(movement.getPosition().getLongitude(), is(-76.32753333333333333333333333));
        assertThat(movement.getTrueHeading(), is(352));
        assertThat(movement.getReportedSpeed(), is(0.0));
        assertThat(movement.getAisPositionAccuracy(), is((short)1));
        int positionSecond = movement.getPositionTime().toInstant().atZone(ZoneOffset.UTC).getSecond();
        assertThat(positionSecond, is(35));
    }
    
    @Test
    public void positionTest() {
        ProcessResult result = processService.processMessages(Arrays.asList(getAisPositionMessage()), new HashSet<>());
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        MovementBaseType movement = movements.get("219024194");
        assertThat(movement.getMmsi(), is("219024194"));
        assertThat(movement.getFlagState(), is("DNK"));
        assertThat(movement.getPosition().getLatitude(), is(57.490381666666664));
        assertThat(movement.getPosition().getLongitude(), is(10.685565));
    }
	
    @Test
    public void onlyFishingVesselsWithNoFishingVesselTest() {
        when(startUp.getSetting("onlyFishingVessels")).thenReturn("true");
        String knownMmsi = "123456789";
        Set<String> fishingVessels = new HashSet<>();
        fishingVessels.add(knownMmsi);
        ProcessResult result = processService.processMessages(Arrays.asList(getAisPositionMessage()), fishingVessels);
        Map<String, MovementBaseType> downsampledMovements = result.getDownsampledMovements();
        assertThat(downsampledMovements.size(), is(0));
    }
	
    @Test
    public void onlyFishingVesselsWithEmptyEmptyKnownFishingVesselsTest() {
        when(startUp.getSetting("onlyFishingVessels")).thenReturn("true");
        Set<String> fishingVessels = new HashSet<>();
        ProcessResult result = processService.processMessages(Arrays.asList(getAisPositionMessage()), fishingVessels);
        Map<String, MovementBaseType> downsampledMovements = result.getDownsampledMovements();
        assertThat(downsampledMovements.size(), is(1));
    }
	
    @Test
    public void notOnlyFishingVesselsWithNoFishingVesselTest() {
        when(startUp.getSetting("onlyFishingVessels")).thenReturn("false");
        String knownMmsi = "123456789";
        Set<String> fishingVessels = new HashSet<>();
        fishingVessels.add(knownMmsi);
        ProcessResult result =processService.processMessages(Arrays.asList(getAisPositionMessage()), fishingVessels);
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        assertThat(movements.size(), is(1));
    }
	
    @Test
    public void notOnlyFishingVesselsWithEmptyKnownFishingVesselsTest() {
        when(startUp.getSetting("onlyFishingVessels")).thenReturn("false");
        Set<String> fishingVessels = new HashSet<>();
        ProcessResult result =processService.processMessages(Arrays.asList(getAisPositionMessage()), fishingVessels);
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        assertThat(movements.size(), is(1));
    }
	
    @Test
    public void positionType18Test() {
        ProcessResult result = processService.processMessages(Arrays.asList(getAisType18Message()), new HashSet<>());
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        MovementBaseType movement = movements.get("338087471");
        assertThat(movement.getMmsi(), is("338087471"));
        assertThat(movement.getFlagState(), is("USA"));
        assertThat(movement.getPosition().getLatitude(), is(40.68454));
        assertThat(movement.getPosition().getLongitude(), is(-74.07213166666666666666666667));
        assertThat(movement.getAisPositionAccuracy(), is((short)0));
    }

    @Test
    public void positionType18WithCommentBlockTest() {
        Sentence aisType1MessageWithCommentBlock = new Sentence("1G2:353911,s:Goteborg,c:1653900489*4E", getAisType18Message().getSentence());
        ProcessResult result = processService.processMessages(Arrays.asList(aisType1MessageWithCommentBlock), new HashSet<>());
        Map<String, MovementBaseType> movements = result.getDownsampledMovements();
        MovementBaseType movement = movements.get("338087471");
        assertThat(movement.getMmsi(), is("338087471"));
        assertThat(movement.getFlagState(), is("USA"));
        assertThat(movement.getPosition().getLatitude(), is(40.68454));
        assertThat(movement.getPosition().getLongitude(), is(-74.07213166666666666666666667));
        assertThat(movement.getAisPositionAccuracy(), is((short) 0));
        ZonedDateTime timestamp = movement.getPositionTime().toInstant().atZone(ZoneOffset.UTC);
        int positionSecond = timestamp.getSecond();
        assertThat(positionSecond, is(49));
        assertThat(timestamp, is(Instant.ofEpochSecond(1653900489).minus(1, ChronoUnit.MINUTES).atZone(ZoneOffset.UTC).withSecond(49)));
    }

    @Test
    public void aisType5Test() {

        ProcessResult result = processService.processMessages(Arrays.asList(getAisType5Message()), new HashSet<>());
        Map<String, AssetDTO> assetMap = result.getDownsampledAssets();
        assertThat(assetMap.size(), is(1));
        AssetDTO asset = assetMap.get("351759000");
        assertThat(asset.getFlagStateCode(), is("PAN"));
        assertThat(asset.getVesselType(), is("Cargo"));
        assertThat(asset.getIrcs(), is("3FOF8"));
    }
    
    @Test
    public void fishingVesselTest() {
        String knownMmsi = "261061000";
        Set<String> fishingVessels = new HashSet<>();
        processService.processMessages(Arrays.asList(getAisType5FishingVessel()), fishingVessels);
        assertThat(fishingVessels.size(), is(1));
        String assetMmsi = fishingVessels.iterator().next();
        assertThat(assetMmsi, is(knownMmsi));
    }
    
    @Test
    public void knownFishingVesselTest() {
        String knownMmsi = "219024194";
        Set<String> fishingVessels = new HashSet<>();
        fishingVessels.add(knownMmsi);
        processService.processMessages(Arrays.asList(getAisPositionMessage()), fishingVessels);
        Mockito.verify(exchangeService).sendToExchange(captor.capture(), Mockito.any());
        List<MovementBaseType> movements = captor.getValue();
        assertThat(movements.size(), is(1));
        assertThat(movements.get(0).getMmsi(), is(knownMmsi));
    }
    
    @Test
    public void notKnownFishingVesselTest() {
        Set<String> fishingVessels = new HashSet<>();
        processService.processMessages(Arrays.asList(getAisPositionMessage()), fishingVessels);
        Mockito.verify(exchangeService).sendToExchange(captor.capture(), Mockito.any());
        List<MovementBaseType> movements = captor.getValue();
        assertThat(movements.size(), is(0));
    }
    
    @Test
    public void aisType24PartATest() {
        ProcessResult result = processService.processMessages(Arrays.asList(getAisType24PartAMessage()), new HashSet<>());
        Map<String, AssetDTO> assetMap = result.getDownsampledAssets();
        assertThat(assetMap.size(), is(1));
        AssetDTO asset = assetMap.get("271041815");
        assertThat(asset.getName(), is("PROGUY"));
    }
    
    @Test
    public void aisType24PartBTest() {
        ProcessResult result = processService.processMessages(Arrays.asList(getAisType24PartBMessage()), new HashSet<>());
        Map<String, AssetDTO> assetMap = result.getDownsampledAssets();
        assertThat(assetMap.size(), is(1));
        AssetDTO asset = assetMap.get("271041815");
        assertThat(asset.getIrcs(), is("TC6163"));
    }
    
    /*
    MessageID:          1
    RepeatIndicator:    0
    UserID:             371798000
    NavigationStatus:   0
    ROT:                -127
    SOG:                12.3
    PositionAccuracy:   1
    longitude:          -123.395383333
    latitude:           48.38163333333
    COG:                224
    TrueHeading:        215
    TimeStamp:          33
    RegionalReserved:   0
    Spare:              0
    RAIM:               False
    state_syncstate:    0
     */
    private Sentence getAisType1Message() {
        return new Sentence(null, "15RTgt0PAso;90TKcjM8h6g208CQ,0*4A");
    }
    
    /*
    MessageID:          2
    RepeatIndicator:    0
    UserID:             356302000
    NavigationStatus:   0
    ROT:                127
    SOG:                13.9
    PositionAccuracy:   0
    longitude:          -71.62614333333333333333333333
    latitude:           40.39235833333333333333333333
    COG:                87.7
    TrueHeading:        91
    TimeStamp:          41
    RegionalReserved:   0
    Spare:              0
    RAIM:               False
    state_syncstate:    0
    state_slottimeout:  3
    state_slotoffset:   6
     */
    private Sentence getAisType2Message() {
        return new Sentence(null, "25Cjtd0Oj;Jp7ilG7=UkKBoB0<06");
    }
    
    /*
    MessageID:          3
    RepeatIndicator:    0
    UserID:             563808000
    NavigationStatus:   5
    ROT:                0
    SOG:                0
    PositionAccuracy:   1
    longitude:          -76.32753333333333333333333333
    latitude:           36.91
    COG:                252
    TrueHeading:        352
    TimeStamp:          35
    RegionalReserved:   0
    Spare:              0
    RAIM:               False
    state_syncstate:    0
    state_slottimeout:  0
    state_slotoffset:   0
     */
    private Sentence getAisType3Message() {
        return new Sentence(null, "38Id705000rRVJhE7cl9n;160000");
    }
    /*
    MessageID:        5
    RepeatIndicator:  0
    UserID:           351759000
    AISversion:       0
    IMOnumber:        9134270
    callsign:         3FOF8  
    name:             EVER DIADEM         
    shipandcargo:     70
    dimA:             225
    dimB:             70
    dimC:             1
    dimD:             31
    fixtype:          1
    ETAminute:        0
    ETAhour:          16
    ETAday:           15
    ETAmonth:         5
    draught:          12.2
    destination:      NEW YORK            
    dte:              0
    Spare:            0
     */
    private Sentence getAisType5Message() {
        return new Sentence(null, "55?MbV02;H;s<HtKR20EHE:0@T4@Dn2222222216L961O5Gf0NSQEp6ClRp8");
    }
    
    /*
    MessageID:          18
    RepeatIndicator:    0
    UserID:             338087471
    Reserved1:          0
    SOG:                0.1
    PositionAccuracy:   0
    longitude:          -74.07213166666666666666666667
    latitude:           40.68454
    COG:                79.6
    TrueHeading:        511
    TimeStamp:          49
    RegionalReserved:   0
    Spare:              0
    RAIM:               True
    CommStateSelector:  1
    CommState:          393222
     */
    private Sentence getAisType18Message() {
        return new Sentence(null, "B52K>;h00Fc>jpUlNV@ikwpUoP06,0*4C");
    }
    
    /*
    MessageID:         24
    RepeatIndicator:   0
    UserID:            271041815
    partnum:           0
    name:              PROGUY 
     */
    private Sentence getAisType24PartAMessage() {
        return new Sentence(null, "H42O55i18tMET00000000000000,2*6D");
    }
    
    /*
    MessageID:         24
    RepeatIndicator:   0
    UserID:            271041815
    partnum:           0
    shipandcargo:      60|
    vendorid:          1D00014
    callsign:          TC6163
    dimA:              0
    dimB:              15
    dimC:              0
    dimD:              5
     */
    private Sentence getAisType24PartBMessage() {
        return new Sentence(null, "H42O55lti4hhhilD3nink000?050,0*40");
    }
    
    private Sentence getAisType5FishingVessel() {
        return new Sentence(null, "5CpuqR029m2U<pLP00084i@T<40000000000000N1HN814lf0<1i6CR@@PC52@ii6CR@@00");
    }

    private Sentence getAisPositionMessage() {
        return new Sentence(null, "13@p;@P0020hrRFPqG5EQUHHP00,0*5C");
    }
}
