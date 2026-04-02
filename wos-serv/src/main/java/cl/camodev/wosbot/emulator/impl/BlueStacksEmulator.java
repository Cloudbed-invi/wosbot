package cl.camodev.wosbot.emulator.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.camodev.wosbot.emulator.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueStacksEmulator extends Emulator {
    private static final Logger logger = LoggerFactory.getLogger(BlueStacksEmulator.class);
    private static final String BS_CONFIG_PATH = "C:\\ProgramData\\BlueStacks_nxt\\bluestacks.conf";

    public BlueStacksEmulator(String consolePath) {
        super(consolePath);
    }

    @Override
    protected String getDeviceSerial(String name) {
        String internalName = resolveInternalName(name);
        String port = findPortInConfig(internalName);
        if (port == null || port.isEmpty()) {
            logger.warn("Could not find ADB port for instance {} in bluestacks.conf, using default 5555", internalName);
            return "127.0.0.1:5555";
        }

        // Check already connected devices to see if it's "127.0.0.1:PORT", "emulator-PORT", or "emulator-PORT-1"
        if (bridge != null && bridge.hasInitialDeviceList()) {
            for (var device : bridge.getDevices()) {
                String serial = device.getSerialNumber();
                int p = Integer.parseInt(port);
                if (serial.equals("127.0.0.1:" + port) || serial.equals("emulator-" + p) || serial.equals("emulator-" + (p - 1))) {
                    logger.debug("Matched BlueStacks serial for {}: {}", internalName, serial);
                    return serial;
                }
            }
        }
        
        return "127.0.0.1:" + port;
    }

    private String resolveInternalName(String name) {
        File configFile = new File(BS_CONFIG_PATH);
        if (!configFile.exists()) {
            return name; 
        }

        Pattern displayPattern = Pattern.compile("bst.instance\\.(.+)\\.display_name=\"(.+)\"");
        String firstInstance = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = displayPattern.matcher(line);
                if (matcher.find()) {
                    String internal = matcher.group(1);
                    String display = matcher.group(2);
                    
                    if (firstInstance == null) {
                        firstInstance = internal;
                    }

                    if (display.equalsIgnoreCase(name) || internal.equalsIgnoreCase(name)) {
                        logger.info("Resolved BlueStacks name '{}' to internal ID: {}", name, internal);
                        return internal;
                    }
                }
            }
            
            // Fallback for default "0" or empty name - use the first instance discovered
            if ((name.equals("0") || name.isEmpty()) && firstInstance != null) {
                logger.info("Using default BlueStacks instance '{}' for identifier '{}'", firstInstance, name);
                return firstInstance;
            }
            
        } catch (IOException e) {
            logger.error("Error resolving name in BlueStacks config", e);
        }
        return name;
    }

    private String findPortInConfig(String internalName) {
        File configFile = new File(BS_CONFIG_PATH);
        if (!configFile.exists()) {
            return null;
        }

        // Search for both bst.instance.<id>.adb_port and bst.instance.<id>.status.adb_port
        String pattern1 = "bst.instance." + internalName + ".adb_port=\"(\\d+)\"";
        String pattern2 = "bst.instance." + internalName + ".status.adb_port=\"(\\d+)\"";
        
        Pattern p1 = Pattern.compile(pattern1);
        Pattern p2 = Pattern.compile(pattern2);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m1 = p1.matcher(line);
                if (m1.find()) return m1.group(1);
                
                Matcher m2 = p2.matcher(line);
                if (m2.find()) return m2.group(1);
            }
        } catch (IOException e) {
            logger.error("Error reading BlueStacks config for port", e);
        }
        return null;
    }

    @Override
    public void launchEmulator(String name) {
        String internalName = resolveInternalName(name);
        
        // Ensure ADB is enabled in config before launching
        ensureAdbEnabled(internalName);
        
        String[] command = { consolePath + File.separator + "HD-Player.exe", "--instance", internalName };
        executeCommand(command);
        logger.info("BlueStacks launched instance: {} (internal: {})", name, internalName);
    }

    /**
     * Automatically enables ADB in bluestacks.conf if it's currently disabled.
     */
    private void ensureAdbEnabled(String internalName) {
        try {
            // Using PowerShell to edit the config file. This handles permissions better 
            // and allows us to perform a safer regex replace on the file contents.
            String search = "bst\\.instance\\." + internalName + "\\.status\\.adb_enable=\"0\"";
            String replace = "bst.instance." + internalName + ".status.adb_enable=\"1\"";
            
            String psCommand = String.format(
                "$path = '%s'; " +
                "$content = Get-Content $path; " +
                "if ($content -match '%s') { " +
                "    $content = $content -replace '%s', '%s'; " +
                "    Set-Content $path $content; " +
                "    Write-Output 'ENABLED'; " +
                "}", 
                BS_CONFIG_PATH, search, search, replace
            );
            
            String[] command = { "powershell.exe", "-Command", psCommand };
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            
            if ("ENABLED".equals(line)) {
                logger.info("Automatically enabled ADB in BlueStacks config for instance: {}", internalName);
            }
        } catch (IOException e) {
            logger.error("Failed to automatically enable ADB for BlueStacks", e);
        }
    }

    @Override
    public void closeEmulator(String name) {
        String internalName = resolveInternalName(name);
        // Kill HD-Player.exe processes that have the specific instance in their command line
        try {
            // Using PowerShell to filter by command line is most reliable
            String psCommand = "Get-Process HD-Player -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*--instance " + internalName + "*' } | Stop-Process -Force";
            String[] command = { "powershell.exe", "-Command", psCommand };
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
            
            logger.info("BlueStacks close command sent for instance: {} (internal: {})", name, internalName);
        } catch (IOException e) {
            logger.error("Error closing BlueStacks emulator", e);
        }
    }

    @Override
    public boolean isRunning(String name) {
        String internalName = resolveInternalName(name);
        String port = findPortInConfig(internalName);

        // 1. Check ADB connectivity (Ultimate truth)
        if (bridge != null && bridge.hasInitialDeviceList() && port != null) {
            for (var device : bridge.getDevices()) {
                String serial = device.getSerialNumber();
                if (device.isOnline() && (serial.equals("127.0.0.1:" + port) || serial.equals("emulator-" + (Integer.parseInt(port) - 1)) || serial.equals("emulator-" + port))) {
                    return true;
                }
            }
        }

        // 2. Resilience check: Check for HD-Player.exe process
        // Simplified check: Just see if ANY HD-Player.exe is running. 
        // For BlueStacks 5, this is usually enough to know it's alive.
        try {
            String[] command = { "tasklist", "/FI", "IMAGENAME eq HD-Player.exe", "/NH" };
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("HD-Player.exe")) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.error("Error checking BlueStacks process with tasklist", e);
        }
        return false;
    }

    private void executeCommand(String[] command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(consolePath));
            pb.start(); // HD-Player doesn't need to be waited for usually as it's the GUI
        } catch (IOException e) {
            logger.error("Error executing BlueStacks command", e);
        }
    }
}
