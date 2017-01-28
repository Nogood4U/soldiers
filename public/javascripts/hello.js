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
    soldier.health = game.add.text(0, 0, "100%", {font: "11px Arial", fill: "#ffffff"});
    soldier.health.anchor.set(0.5);
    soldier.nameTag = game.add.text(0, 0, _playerId, {font: "15px Arial", fill: "#ffffff"});
    soldier.nameTag.anchor.set(0.5);
    //soldier.addChild(soldier.health)
    players[_playerId] = soldier;
    return soldier;
}

function initGame(playerId) {
    let preload = function () {
        game.load.spritesheet('soldierSelf', '/assets/images/sprites/soldierGun.png', 16, 16);
        game.load.spritesheet('weapons', '/assets/images/sprites/weapons.png', 24, 24);
        game.load.spritesheet('bullet', '/assets/images/sprites/bullet.png', 19, 19);
        game.load.image('background', '/assets/images/background/back_3.png');
        game.load.audio('map', ['assets/music/Mercury.mp3', 'assets/audio/Mercury.ogg']);
        game.load.audio('fire', ['assets/music/laser.mp3', 'assets/audio/laser.ogg']);
        _server = server("server", playerId);

    };

    let create = function () {
        bg = game.add.tileSprite(0, 0, 1500, 1500, 'background');
        weapon = game.add.sprite(700, 0, 'weapons');
        weapon.scale.setTo(3.125, 3.125);
        weapon.frame = 0;
        weapon.fixedToCamera = true;
        score = game.add.text(600, 0, "100%", {font: "32px Arial", fill: "#ffffff"});
        score.fixedToCamera = true;
        let soldierself = createPlayer(playerId);
        cursors = game.input.keyboard.createCursorKeys();
        fireButton = game.input.keyboard.addKey(Phaser.Keyboard.SPACEBAR);
        shift = game.input.keyboard.addKey(Phaser.Keyboard.SHIFT);
        _server.then(obj => obj.stream.subscribe((val) => {
            val.players.forEach((serverPlayer) => {
                if (serverPlayer.playerId) {
                    let localPlayer = players[serverPlayer.playerId];
                    if (!localPlayer) {
                        localPlayer = createPlayer(serverPlayer.playerId);
                    }
                    localPlayer.updated = true;
                    game.add.tween(localPlayer).to(
                        {
                            x: mtToPx(serverPlayer.posX),
                            y: mtToPx(serverPlayer.posY)
                        },
                        16,
                        Phaser.Easing.LINEAR,
                        true
                    );
                    game.add.tween(localPlayer.health).to(
                        {
                            x: mtToPx(serverPlayer.posX),
                            y: mtToPx(serverPlayer.posY) - localPlayer.height / 2
                        },
                        16,
                        Phaser.Easing.LINEAR,
                        true
                    );
                    game.add.tween(localPlayer.nameTag).to(
                        {
                            x: mtToPx(serverPlayer.posX),
                            y: mtToPx(serverPlayer.posY) - localPlayer.height / 2 - 15
                        },
                        16,
                        Phaser.Easing.LINEAR,
                        true
                    );
                    localPlayer.health.text = serverPlayer.health + "%";
                    if (!serverPlayer.viewOr && localPlayer.scale.x > 0)
                        localPlayer.scale.x *= -1;
                    else if (serverPlayer.viewOr && localPlayer.scale.x < 0)
                        localPlayer.scale.x *= -1;

                    if (serverPlayer.playerId === playerId) {
                        score.text = serverPlayer.health + "%";
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
            });
            val.bullets.forEach((serverPlayer) => {
                if (serverPlayer.ownerId) {
                    if (bulletsMap[serverPlayer.bulletNum]) {
                        game.add.tween(bulletsMap[serverPlayer.bulletNum]).to(
                            {
                                x: mtToPx(serverPlayer.posX),
                                y: mtToPx(serverPlayer.posY)
                            },
                            16,
                            Phaser.Easing.LINEAR,
                            true
                        );
                        /* bulletsMap[serverPlayer.bulletNum].x = mtToPx(serverPlayer.posX);
                         bulletsMap[serverPlayer.bulletNum].y = mtToPx(serverPlayer.posY);*/
                    } else {
                        let bullet = bullets.getFirstExists(false);
                        if (bullet) {
                            //  Grab the first bullet we can from the pool
                            bullet.reset(mtToPx(serverPlayer.posX), mtToPx(serverPlayer.posY));
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
            Object.entries(players).forEach(([key, player]) => {
                if (!player.updated) {
                    player.destroy();
                    delete players[player];
                } else {
                    player.updated = false;
                }
            });
        }));
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
                gunFire.play();
            }
            commands.btns = btns;
            _server.then(obj => {
                if (obj.ws.readyState === 1 && send)
                    obj.ws.sendX(JSON.stringify(commands));
            });
        };
        //  Our bullet group
        bullets = game.add.group();
        bullets.createMultiple(100, 'bullet');
        bullets.setAll('anchor.x', 0.5);
        bullets.setAll('anchor.y', 0.5);
        music = game.add.audio('map');
        gunFire = game.add.audio('fire');
        gunFire.volume = 0.3;
        music.loopFull(0.1);
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
        commands.btns = btns;
        _server.then(obj => {
            if (obj.ws.readyState === 1 && send)
                obj.ws.sendX(JSON.stringify(commands));
        });
        // game.debug.cameraInfo(game.camera, 32, 32);
        game.debug.spriteInfo(players[playerId], 32, 32);
        game.debug.text(game.time.fps || '--', 13, 200, "#00ff00");
    };
    game = new Phaser.Game(800, 600, Phaser.AUTO, '', {preload: preload, create: create, update: update});

}

