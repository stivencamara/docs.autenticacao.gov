#include "eidlib.h"
#include "eidlibException.h"
#include <iostream>
#include <string.h>

using namespace eIDMW;

int main(int argc, char **argv) {

    try 
    {
        //Initializes SDK (must always be called in the beginning of the program)
        PTEID_InitSDK();

        //Enable test mode if you're using a test card
        PTEID_Config::SetTestMode(true);

        //Gets the set of readers connected to the system
        PTEID_ReaderSet& readerSet = PTEID_ReaderSet::instance();

        unsigned long reader_count = ReaderSet.readerCount();
        const char * const * reader_list = ReaderSet.readerList();
        std::cout << "Connected Readers: " << std::endl;

        for (int i=0; i < reader_count; i++) {
            std::cout << i << ": " << reader_list[i] << std::endl;
        }

        //Gets a reader connected to the system (useful if you only have one)
        //Alternatively you can iterate through the readers using getReaderByNum(int index) instead of getReader()
        PTEID_ReaderContext& reader = readerSet.getReader();

        //Gets the Card Contact Interface and type
        PTEID_CardContactInterface contactInterface;
        PTEID_CardType cardType;

        //Gets the Card Contact Interface and type
        if(reader.isCardPresent()) {
            contactInterface = reader.getCardContactInterface();
            cardType = reader.getCardType();
            std::cout << "Contact Interface:" << (contactInterface == PTEID_CARD_CONTACTLESS ? "CONTACTLESS" : "CONTACT") << std::endl;
        }

        //Gets the EIDCard 
        PTEID_EIDCard& eidCard = reader.getEIDCard();

        //If the contactInterface is contactless and the card supports contactless then authenticate with PACE
        if (contactInterface == PTEID_CARD_CONTACTLESS && cardType == PTEID_CARDTYPE_IAS5){
            std::string can_str;
            std::cout << "Insert the Card access number (CAN) for the card in use: ";
            std::cin >> can_str;
            eidCard.initPaceAuthentication(can_str.c_str(), can_str.size(),  PTEID_CardPaceSecretType::PTEID_CARD_SECRET_CAN);
        }

        //Gets the EId objects (with the cards information)
        PTEID_EId& eid = eidCard.getID();

        std::cout << "Name:                       " << eid.getGivenName() << " " << eid.getSurname() << std::endl;
        std::cout << "Card Type:                  " << eid.getDocumentType() << std::endl;
        std::cout << "Card Version:               " << eid.getDocumentVersion() << std::endl;
        std::cout << "Card Number:                " << eid.getDocumentNumber() << std::endl;
        std::cout << "Local of Request:           " << eid.getLocalofRequest() << std::endl;   
        std::cout << "Issuing Entity:             " << eid.getIssuingEntity() << std::endl;
        std::cout << "Issuing Date:               " << eid.getValidityBeginDate() << std::endl;
        std::cout << "Validity End Date:          " << eid.getValidityEndDate() << std::endl;
        std::cout << "PAN Number:                 " << eid.getDocumentPAN() << std::endl;
        std::cout << "Civilian ID :               " << eid.getCivilianIdNumber() << std::endl;
        std::cout << "Tax ID:                     " << eid.getTaxNo() << std::endl;
        std::cout << "Social Security ID:         " << eid.getSocialSecurityNumber() << std::endl;
        std::cout << "Health ID:                  " << eid.getHealthNumber() << std::endl;
        std::cout << "Parents:                    " << eid.getParents() << std::endl;
        std::cout << "Father:                     " << eid.getGivenNameFather() << " " << eid.getSurnameFather() << std::endl;
        std::cout << "Mother:                     " << eid.getGivenNameMother() << " " << eid.getSurnameMother() << std::endl;
        std::cout << "Indications:                " << eid.getAccidentalIndications() << std::endl;
        std::cout << "Nationality:                " << eid.getNationality() << std::endl;
        std::cout << "Country:                    " << eid.getCountry() << std::endl;
        std::cout << "Date of birth:              " << eid.getDateOfBirth() << std::endl;
        std::cout << "Height:                     " << eid.getHeight() << std::endl;
        std::cout << "Gender:                     " << eid.getGender() << std::endl;
        std::cout << "MRZ (Machine Readable Zone): " << eid.getMRZ1() << std::endl;
        std::cout << "                             " << eid.getMRZ2() << std::endl;
        std::cout << "                             " << eid.getMRZ3() << std::endl;   
        std::cout << (cardType == PTEID_CARDTYPE_IAS5 ? "CC2" : "CC1") << std::endl; 
    }
    catch (PTEID_ExNoReader &e) 
    {
        std::cerr << "No readers found!" << std::endl;
    }
    catch (PTEID_ExNoCardPresent &e) 
    {
        std::cerr << "No card found in the reader!" << std::endl;
    }
    catch (PTEID_Exception &e) 
    {
        std::cerr << "Caught exception in some SDK method. Error: " << e.GetMessage() << std::endl;
    }

    //Releases SDK (must always be called at the end of the program)
    PTEID_ReleaseSDK();
    return 0;
}
