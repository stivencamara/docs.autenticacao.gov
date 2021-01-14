#!/bin/bash
# autenticacao.gov and docs.autenticacao.gov repositories clones have to be at same level
VERSION=$(awk -F "=" '/PTEID_PRODUCT_VERSION=/{print $2}' ../autenticacao.gov/pteid-mw-pt/_src/eidmw/release_data)
NOW=$(date +"%d/%m/%Y")

#filter out header, table of contents and css class .center
echo "Removing header 'page', table of contents and css 'center' image class."
perl -p0e "s/.*Content_begin -->//s" manual_sdk.md > manual_sdk_no_header.md
perl -p0e "s/End_of_content.*//s" manual_sdk_no_header.md > manual_sdk_no_header_no_footer.md
sed 's/{:.center}//g' manual_sdk_no_header_no_footer.md > manual_sdk_filtered.md

#generate pdf from ast
echo "Generating pdf manual."
pandoc -f gfm manual_sdk_filtered.md -o Manual_de_SDK.pdf --template=template_sdk.tex --toc --number-sections --variable version=$VERSION --variable date=$NOW --pdf-engine=xelatex --listings --variable colorlinks=true


if [ $? -eq 0 ]
then echo "Done. PDF file Manual_de_SDK.pdf created."
else
    echo "An error has ocurred. PDF file was not created."
fi

#clean-up
rm manual_sdk_no_header.md manual_sdk_no_header_no_footer.md manual_sdk_filtered.md
