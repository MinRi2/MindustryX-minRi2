package mindustryX.features;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.toolpack.ArcScanMode
public class ArcTransportScanMode{
    static final int maxLoop = 200;

    private static final Seq<Point> path = new Seq<>();

    public static void draw(){
        Draw.z(Layer.overlayUI);

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(hoverTile == null || hoverTile.build == null || !hoverTile.build.isDiscovered(player.team())){
            return;
        }

        path.clear();
        travelPath(new Point(hoverTile.build, null), ArcTransportScanMode::getPrevious);
        drawPath(false);

        path.clear();
        travelPath(new Point(hoverTile.build, null), ArcTransportScanMode::getNext);
        drawPath(true);
    }

    private static void travelPath(Point point, Func<Point, Seq<Point>> getNext){
        if(point.build == null || path.size > maxLoop) return;
        if(!point.trans) return;

        Point same = path.find(other -> point.build == other.build && (other.from == null || point.from.build == other.from.build));
        if(same != null){
            if(point.conduit >= same.conduit) return;
            else path.replace(same, point);
        }else path.add(point);

        getNext.get(point).each(p -> travelPath(p, getNext));
    }

    private static Seq<Point> getPrevious(Point point){
        Building build = point.build;
        if(build == null) return new Seq<>();
        Seq<Point> previous = new Seq<>();
        //质驱
        if(build instanceof MassDriver.MassDriverBuild){
            //暂时搞不定
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild bridge){
            bridge.incoming.each(pos -> previous.add(new Point(world.tile(pos).build, point)));
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild bridge){
            for(Building b : bridge.occupied){
                if(b != null){
                    previous.add(new Point(b, point));
                }
            }
        }
        for(Building b : build.proximity){
            Point from = new Point(b, b.relativeTo(build), b.block.instantTransfer ? point.conduit + 1 : 0, point);
            if(canInput(point, b, true) && canOutput(from, build, false)){
                previous.add(from);
            }else if(canOutput(from, build, false)){
                from.trans = false;
                previous.add(from);
            }
        }
        return previous;
    }

    private static Seq<Point> getNext(Point point){
        Building build = point.build;
        if(build == null) return new Seq<>();
        Seq<Point> next = new Seq<>();
        //质驱
        if(build instanceof MassDriver.MassDriverBuild massDriverBuild){
            if(massDriverBuild.arcLinkValid()){
                next.add(new Point(world.build(massDriverBuild.link), point));
            }
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild itemBridgeBuild){
            if(itemBridgeBuild.arcLinkValid()){
                next.add(new Point(world.build(itemBridgeBuild.link), point));
            }
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild){
            DirectionBridge.DirectionBridgeBuild link = directionBridgeBuild.findLink();
            if(link != null){
                next.add(new Point(link, point));
            }
        }

        for(Building b : build.proximity){
            Point to = new Point(b, build.relativeTo(b), b.block.instantTransfer ? point.conduit + 1 : 0, point);
            if(canInput(to, build, false) && canOutput(point, b, true)){
                next.add(to);
            }else if(canInput(to, build, false)){
                to.trans = false;
                next.add(to);
            }
        }
        return next;
    }

    private static boolean canInput(Point point, Building from, boolean active){
        Building build = point.build;
        if(build == null || from == null) return false;
        if(from.block.instantTransfer && point.conduit > 2) return false;
        //装甲传送带
        if(build instanceof ArmoredConveyor.ArmoredConveyorBuild){
            return from != build.front() && (from instanceof Conveyor.ConveyorBuild || from == build.back());
        }//装甲导管
        else if(build instanceof Duct.DuctBuild ductBuild && ((Duct)ductBuild.block).armored){
            return from != build.front() && (from.block.isDuct || from == build.back());
        }//传送带和导管
        else if(build instanceof Conveyor.ConveyorBuild || build instanceof Duct.DuctBuild){
            return from != build.front();
        }//塑钢带
        else if(build instanceof StackConveyor.StackConveyorBuild stackConveyorBuild){
            return switch(stackConveyorBuild.state){
                case 2 -> from == build.back() && from instanceof StackConveyor.StackConveyorBuild;
                case 1 -> from != build.front();
                default -> from instanceof StackConveyor.StackConveyorBuild;
            };
        }//交叉器
        else if(build instanceof Junction.JunctionBuild){
            return point.facing == -1 || from.relativeTo(build) == point.facing;
        }//分类
        else if(build instanceof Sorter.SorterBuild sorterBuild){
            return !active || build.relativeTo(from) != point.facing && (sorterBuild.sortItem != null || (from.relativeTo(build) == point.facing) == ((Sorter)sorterBuild.block).invert);
        }//溢流
        else if(build instanceof OverflowGate.OverflowGateBuild){
            return !active || build.relativeTo(from) != point.facing;
        }//导管路由器与导管溢流
        else if(build instanceof DuctRouter.DuctRouterBuild || build instanceof OverflowDuct.OverflowDuctBuild){
            return from == build.back();
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild itemBridgeBuild){
            if(build.block instanceof LiquidBridge && !from.block.hasLiquids) return false;
            return itemBridgeBuild.arcCheckAccept(from);
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild){
            return directionBridgeBuild.arcCheckAccept(from);
        }else if(build instanceof Router.RouterBuild){
            return true;
        }else if(canAccept(build.block)){
            point.trans = false;
            return true;
        }
        return false;
    }


    private static boolean canAccept(Block block){
        if(block.group == BlockGroup.transportation) return true;
        for(Item item : content.items()){
            if(block.consumesItem(item) || block.itemCapacity > 0){
                return true;
            }
        }
        return false;
    }

    private static boolean canOutput(Point point, Building to, boolean active){
        Building build = point.build;
        if(build == null || to == null) return false;
        if(to.block.instantTransfer && point.conduit > 2) return false;
        //传送带和导管
        if(build instanceof Conveyor.ConveyorBuild || build instanceof Duct.DuctBuild){
            return to == build.front();
        }//塑钢带
        else if(build instanceof StackConveyor.StackConveyorBuild stackConveyor){
            if(stackConveyor.state == 2 && ((StackConveyor)stackConveyor.block).outputRouter){
                return to != build.back();
            }
            return to == build.front();
        }//交叉器
        else if(build instanceof Junction.JunctionBuild){
            return point.facing == -1 || build.relativeTo(to) == point.facing;
        }//分类
        else if(build instanceof Sorter.SorterBuild sorterBuild){
            return !active || to.relativeTo(build) != point.facing && (sorterBuild.sortItem != null || (build.relativeTo(to) == point.facing) == ((Sorter)sorterBuild.block).invert);
        }//溢流
        else if(build instanceof OverflowGate.OverflowGateBuild){
            return !active || to.relativeTo(build) != point.facing;
        }//导管路由器与导管溢流
        else if(build instanceof DuctRouter.DuctRouterBuild || build instanceof OverflowDuct.OverflowDuctBuild){
            return to != build.back();
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild bridge){
            if(build.block instanceof LiquidBridge && !to.block.hasLiquids) return false;
            return bridge.arcCheckDump(to);
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild){
            DirectionBridge.DirectionBridgeBuild link = directionBridgeBuild.findLink();
            return link == null && build.relativeTo(to) == build.rotation;
        }else if(build instanceof Router.RouterBuild || build instanceof Unloader.UnloaderBuild){
            return true;
        }else if(build instanceof GenericCrafter.GenericCrafterBuild){
            point.trans = false;
            return true;
        }
        return false;
    }

    private static void drawPath(boolean forward){
        Color mainColor = forward ? Color.valueOf("80ff00") : Color.valueOf("ff8000");
        Color highlightColor = forward ? Color.valueOf("00cc00") : Color.red;
        path.each(p -> {
            if(p.from != null && p.trans){
                float x1 = p.build.tile.drawx(), y1 = p.build.tile.drawy();
                float x2 = p.from.build.tile.drawx(), y2 = p.from.build.tile.drawy();

                Draw.color(mainColor);
                Draw.color(Tmp.c1.set(mainColor).a(Mathf.absin(4f, 1f) * 0.4f + 0.6f));
                Lines.stroke(1.5f);
                Lines.line(x1, y1, x2, y2);
            }else{
                Drawf.selected(p.build, Tmp.c1.set(highlightColor).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f));
            }
            Draw.reset();
        });
        path.each(p -> {
            if(p.from != null && p.trans){
                float x1 = p.build.tile.drawx(), y1 = p.build.tile.drawy();
                float x2 = p.from.build.tile.drawx(), y2 = p.from.build.tile.drawy();
                float dst = Mathf.dst(x1, y1, x2, y2);

                Draw.color(highlightColor);
                Fill.circle(x1, y1, 1.8f);

                if(dst > tilesize){
                    Draw.color(highlightColor);
                    Vec2 to = Tmp.v1.set(x1, y1), from = Tmp.v2.set(x2, y2);
                    if(!forward){
                        from.set(to);
                        to.set(x2, y2);
                    }
                    //在中间位置，按朝向方向绘制一个三角形
                    Vec2 v = to.div(from).scl(0.5f);
                    Fill.poly(v.x + from.x, v.y + from.y, 3, 3f, v.angle());
                }
            }
            Draw.reset();
        });
    }

    private static class Point{
        public final Building build;
        public byte facing = -1;
        public int conduit = 0;
        //用于记录端点方块
        public boolean trans = true;

        public final Point from;

        public Point(Building build, Point from){
            this.build = build;
            this.from = from;
        }

        public Point(Building build, byte facing, int conduit, Point from){
            this.build = build;
            this.facing = facing;
            this.conduit = conduit;
            this.from = from;
        }
    }
}
