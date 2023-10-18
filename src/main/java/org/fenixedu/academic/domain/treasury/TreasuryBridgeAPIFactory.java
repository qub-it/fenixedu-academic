package org.fenixedu.academic.domain.treasury;

public class TreasuryBridgeAPIFactory {

    private static ITreasuryBridgeAPI _impl;
    
    public static ITreasuryBridgeAPI implementation() {
        return _impl;
    }
    
    public static void registerImplementation(ITreasuryBridgeAPI impl) {
        _impl = impl;
    }
}