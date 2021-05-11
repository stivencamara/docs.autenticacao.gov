﻿using System;
using System.IO;
using pt.portugal.eid;

namespace Examples
{
    class SignFile
    {
        //Main attributes needed for SDK functionalities
        PTEID_ReaderSet readerSet = null;
        PTEID_ReaderContext readerContext = null;
        PTEID_EIDCard eidCard = null;
        PTEID_EId eid = null;


        /*
         * Initializes the SDK and sets main variables
         */
        public void Initiate()
        {
            //Must always be called in the beginning of the program
            PTEID_ReaderSet.initSDK();

            //Gets the set of connected readers, if there is any inserted
            readerSet = PTEID_ReaderSet.instance();

            //Gets the first reader
            //When multiple readers are connected, you should iterate through the various indexes with the methods getReaderName and getReaderByName
            readerContext = readerSet.getReader();

            //Gets the card instance
            eidCard = readerContext.getEIDCard();
            eid = eidCard.getID();
        }

        /*
         * Releases the SDK (must always be done at the end of the program)
         */
        public void Release()
        {
            try
            {
                PTEID_ReaderSet.releaseSDK();
            }
            catch (PTEID_Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        /*
         * Signs a pdf file
         */
        public void Sign(String input_file, String output_file)
        {

            //To sign a document you must initialize an instance of PTEID_PDFSignature 
            //It takes the path for the input file as argument
            PTEID_PDFSignature signature = new PTEID_PDFSignature(input_file);

            //You can set the location and reason of signature by simply changing this strings
            String location = "Lisboa, Portugal";
            String reason = "Concordo com o conteudo do documento";

            //The page and coordinates where the signature will be printed
            int page = 1;
            double pos_x = 0.1;
            double pos_y = 0.1;

            //To actually sign the document you invoke this method, your authentication PIN will be requested
            //After this you can check the signed document in the path provided
            eidCard.SignPDF(signature, page, pos_x, pos_y, location, reason, output_file);

            Console.WriteLine("File signed with success.");
        }

        public void start(string[] args)
        {
            try
            {
                Initiate();
                Sign(args[0], args[1]);
            }
            catch (PTEID_ExNoReader)
            {
                Console.WriteLine("No reader found.");
            }
            catch (PTEID_ExNoCardPresent)
            {
                Console.WriteLine("No card inserted.");
            }
            catch (PTEID_Exception ex)
            {
                Console.WriteLine(ex.GetMessage());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
            finally
            {
                Release();
                Console.ReadLine();
            }
        }

        static void Main(string[] args)
        {
            new SignFile().start(args);
        }
    }
}
