package edu.gtri.gpssample.constants

enum class ResultCode( val value: Int ) {
    GenerateBarcode(1001 ),
    BarcodeScanned( 1002 ),
    ConfigurationCreated( 1003 )
}