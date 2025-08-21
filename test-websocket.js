const WebSocket = require('ws');

console.log('Testing Java WebSocket Signalling Server...\n');

// Test different connection types
const testConnections = [
    {
        name: 'Streamer',
        url: 'ws://127.0.0.1:8888/type=streamer&insid=675ba9d5b1796fc2539d3930&projectid=6821C61B48A925890040C3B3849B06C3'
    },
    {
        name: 'Player', 
        url: 'ws://127.0.0.1:8888/type=player&insid=123456&projectid=ABCDEF'
    },
    {
        name: 'SFU',
        url: 'ws://127.0.0.1:8888/type=sfu&insid=sfu001&projectid=TESTPROJECT'
    }
];

function testConnection(connectionInfo) {
    return new Promise((resolve) => {
        console.log(`Testing ${connectionInfo.name} connection...`);
        console.log(`URL: ${connectionInfo.url}`);
        
        const ws = new WebSocket(connectionInfo.url);
        
        ws.on('open', function() {
            console.log(`✓ ${connectionInfo.name} connected successfully`);
            
            // Send a ping message
            const ping = {
                type: 'ping',
                time: Date.now()
            };
            ws.send(JSON.stringify(ping));
            console.log(`  Sent: ${JSON.stringify(ping)}`);
            
            // Wait a bit for response, then close
            setTimeout(() => {
                ws.close();
            }, 2000);
        });
        
        ws.on('message', function(data) {
            console.log(`  Received: ${data}`);
        });
        
        ws.on('close', function() {
            console.log(`✓ ${connectionInfo.name} connection closed\n`);
            resolve();
        });
        
        ws.on('error', function(error) {
            console.log(`✗ ${connectionInfo.name} connection failed: ${error.message}\n`);
            resolve();
        });
        
        // Timeout after 5 seconds
        setTimeout(() => {
            if (ws.readyState === WebSocket.CONNECTING) {
                console.log(`✗ ${connectionInfo.name} connection timeout\n`);
                ws.terminate();
                resolve();
            }
        }, 5000);
    });
}

async function runTests() {
    for (const connection of testConnections) {
        await testConnection(connection);
    }
    console.log('All tests completed!');
}

runTests().catch(console.error);