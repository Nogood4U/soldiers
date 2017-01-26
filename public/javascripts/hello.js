var _server = {};
var players = {};
var bg;
var bulletsMap = {}
/*xMeters = 0.02f * xPixels;
 yMeters = 0.02f * yPixels;

 In reverse:

 xPixels = 50.0f * xMeters;
 yPixels = 50.0f * yMeters;*/

function pxToMt(unitPixel) {
    return unitPixel * 0.02;
}


function mtToPx(unitMeters) {
    return unitMeters * 50.0;
}

var game = {};

function createPlayer(_playerId) {
    let soldier = game.add.sprite(0, 0, 'soldierSelf');
    soldier.scale.setTo(3.125, 3.125);
    soldier.anchor.setTo(0.5, 0.5);
    soldier.animations.add("r", [17, 18, 19, 20, 21, 22]);
    soldier.animations.play('r', 9, true);
    players[_playerId] = soldier;
    return soldier;
}

function initGame(playerId) {
    let preload = function () {
        game.load.spritesheet('soldierSelf', '/assets/images/sprites/soldierGun.png', 16, 16);
        game.load.spritesheet('weapons', '/assets/images/sprites/weapons.png', 24, 24);
        game.load.spritesheet('bullet', '/assets/images/sprites/bullet.png', 19, 19);
        game.load.image('background', '/assets/images/background/bg.jpg');

        _server = server("server", playerId);

    };

    let create = function () {
        bg = game.add.tileSprite(0, 0, 1500, 1500, 'background');
        weapon = game.add.sprite(700, 0, 'weapons');
        weapon.scale.setTo(3.125, 3.125);
        weapon.frame = 0;
        weapon.fixedToCamera = true;
        score = game.add.text(600, 0, "100%",{ font: "32px Arial", fill: "#ffffff"});
        score.fixedToCamera = true;
        let soldierself = createPlayer(playerId);
        bitmap = this.game.add.bitmapData(1500, 1500);
        bitmap.context.fillStyle = 'rgb(255, 255, 255)';
        bitmap.context.strokeStyle = 'rgb(255, 255, 255)';
        game.add.image(0, 0, bitmap);

        cursors = game.input.keyboard.createCursorKeys();
        fireButton = game.input.keyboard.addKey(Phaser.Keyboard.SPACEBAR);
        shift = game.input.keyboard.addKey(Phaser.Keyboard.SHIFT);
        _server.stream.subscribe((val) => {
            val.players.forEach((serverPlayer) => {
                if (serverPlayer.playerId) {
                    let localPlayer = players[serverPlayer.playerId];
                    if (!localPlayer) {
                        localPlayer = createPlayer(serverPlayer.playerId);
                    }
                    localPlayer.x = mtToPx(serverPlayer.posX);
                    localPlayer.y = mtToPx(serverPlayer.posY);
                    if(serverPlayer.playerId === playerId){
                        score.text = serverPlayer.health +"%";
                        switch (serverPlayer.currWpn) {
                            case 1:
                                weapon.frame = 0;
                                break;
                            case 2:
                                weapon.frame = 6;
                                break;
                            case 3:
                                weapon.frame = 9;
                                break;
                        }
                    }
                }
                if (serverPlayer.ownerId) {


                    if (bulletsMap[serverPlayer.bulletNum]) {
                        bulletsMap[serverPlayer.bulletNum].x = mtToPx(serverPlayer.posX);
                        bulletsMap[serverPlayer.bulletNum].y = mtToPx(serverPlayer.posY);
                    } else {
                        let bullet = bullets.getFirstExists(false);
                        if (bullet) {
                            //  Grab the first bullet we can from the pool
                            bullet.reset(mtToPx(serverPlayer.posX), mtToPx(serverPlayer.posY) + 8);
                            bullet.x = mtToPx(serverPlayer.posX);
                            bullet.y = mtToPx(serverPlayer.posY);
                            bulletsMap[serverPlayer.bulletNum] = bullet;
                        }
                    }
                    bulletsMap[serverPlayer.bulletNum].updated = true;
                }
            });
            Object.entries(bulletsMap).forEach(([key, bullet]) => {
                if (!bullet.updated) {
                    bullet.kill();
                    delete bulletsMap[key];
                } else {
                    bullet.updated = false;
                }
            });
        });
        game.world.setBounds(0, 0, 1500, 1500);
        game.camera.follow(soldierself);
        game.time.advancedTiming = true;
        game.input.keyboard.onDownCallback = function () {
            let commands = {
                "xMv": 0,
                "yMv": 0,
                "weapon": 0,
                "viewOr": 0,
                "btns": []
            };
            let send = false;
            let btns = [];
            if (game.input.keyboard.event.keyCode == Phaser.Keyboard.SHIFT) {
                btns.push("shift");
                send = true;
            }
            if (game.input.keyboard.event.keyCode == Phaser.Keyboard.SPACEBAR) {
                btns.push("fire");
                send = true;
            }
            commands.btns = btns;
            if (_server.ws.readyState === 1 && send)
                _server.ws.send(JSON.stringify(commands));
        };
        //  Our bullet group
        bullets = game.add.group();
        bullets.createMultiple(50, 'bullet');
        bullets.setAll('anchor.x', 0.5);
        bullets.setAll('anchor.y', 1);
        bullets.setAll('outOfBoundsKill', true);
        bullets.setAll('checkWorldBounds', true);

    };


    let update = function () {
        let btns = [];
        let commands = {
            "xMv": 0,
            "yMv": 0,
            "weapon": 0,
            "viewOr": 1
        };
        let send = false;
        if (cursors.up.isDown) {
            commands.yMv = -10;
            send = true;
        }
        else if (cursors.down.isDown) {
            commands.yMv = 10;
            send = true;
        }

        if (cursors.left.isDown) {
            commands.xMv = -10;
            send = true;
        }
        else if (cursors.right.isDown) {
            commands.xMv = 10;
            send = true;
        }


        if (fireButton.isDown) {
            send = true;
            bitmap.context.clearRect(0, 0, 1500, 1500);
            bitmap.context.globalAlpha = 0.3;
            bitmap.context.fillStyle = 'rgba(255, 255, 255, 0.5)';
            for (var i = 0; i < 700; i++) {
                bitmap.context.fillRect(players[playerId].x + players[playerId].width / 2 + i + 15, players[playerId].y, 3, 3);
            }

            bitmap.dirty = true;
        } else {
            bitmap.context.clearRect(0, 0, 1500, 1500);
            bitmap.dirty = true;
        }
        commands.btns = btns;
        if (_server.ws.readyState === 1 && send)
            _server.ws.send(JSON.stringify(commands));

        // game.debug.cameraInfo(game.camera, 32, 32);
        game.debug.spriteInfo(players[playerId], 32, 32);
        game.debug.text(game.time.fps || '--', 13, 200, "#00ff00");
    };
    game = new Phaser.Game(800, 600, Phaser.AUTO, '', {preload: preload, create: create, update: update});

}

